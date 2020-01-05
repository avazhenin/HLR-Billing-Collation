/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hlr.billing.collation;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author vazhenin
 */
public class Collation implements Runnable {

    ArrayList<MainFrame.SubsData> subsList;
    Worker worker;

    /* database connection parameters */
    String user, pwd, sid, ip, port;

    public Collation(ArrayList<MainFrame.SubsData> subsList, Worker w) {
        this.subsList = subsList;
        this.worker = w;
    }

    @Override
    public void run() {
        int subsProcessed = 0;
        worker.establishConnection();
        worker.loginToHLR();

        Connection conn = null;
        PreparedStatement st = null;
        try {
            conn = DriverManager.getConnection("jdbc:oracle:thin:@10.254.5.10:1521:BER1", "contour", "contour7w");
        } catch (Exception e) {
            e.printStackTrace();
        }

        MainFrame.addLog("Thread " + Thread.currentThread().getName() + " started; Subs to process : " + subsList.size());
        long StartTime = new Date().getTime() / 1000;
        try {
            for (int i = 0; i < subsList.size(); i++) {
                MainFrame.SubsData get = subsList.get(i);

                String st_id = get.st_id;
                String phone = "7" + get.msisdn;
                String imsi = get.usi;
                String subsData = worker.getSubsDataByMSISDN(phone);
                int subs_id = Integer.valueOf(get.subs_id);
                String hlrimsi = new String();

                if (subsData != null) {
                    if (!subsData.contains(phone)) {
                        System.out.println("Error getting data for " + phone);
                    }
                    /* check subscriber IMSI */
                    hlrimsi = worker.parseIMSI(subsData);
                    if (!hlrimsi.equalsIgnoreCase(imsi)) {
                        MainFrame.insertRow(phone, "Несоответствие IMSI");
                    }

                    /* check subs type PrePaid or PostPaid */
                    int invoiceSubsType = Integer.parseInt(st_id);
                    int pdType = worker.parsePDChargeType(subsData);
                    int voiceType = worker.parseVoiceChargeType(subsData);

                    if (pdType != voiceType) {
                        String comm = " тип обслуживания для ПД и Голоса на HLR не одинаковый";
                        MainFrame.insertRow(phone, comm);
                    } else {
                        if (pdType != invoiceSubsType) {
                            String comm = " услуга ПД на HLR не соответствует типу обслуживания абонента в инвойс";
                            MainFrame.insertRow(phone, comm);
                        }
                        if (voiceType != invoiceSubsType) {
                            String comm = " услуга Голоса на HLR не соответствует типу обслуживания абонента в инвойс";
                            MainFrame.insertRow(phone, comm);
                        }
                    }

                    /* check all subscriber services */
                    String sql2 = "select ssh.serv_id, ssh.sstat_id\n"
                            + "  from subs_serv_history ssh\n"
                            + " where ssh.subs_id = ? and sysdate between ssh.stime and ssh.etime - 1 / 86400";
                    st = conn.prepareStatement(sql2);
                    st.setInt(1, subs_id);
                    st.executeQuery();
                    ResultSet rs2 = st.getResultSet();
                    /* switch SERV_ID */
                    while (rs2.next()) {
                        switch (rs2.getInt("serv_id")) {
                            /* outgoing telephony */
                            case 1:
                                int status = worker.parseOutgoingTelephony(subsData);
                                if (status != -1) {
                                    /* if telephony has blocked or turned off status then we consider it as status=2 */
                                    int invStatus = rs2.getInt("sstat_id");
                                    if (invStatus == 2 || invStatus == 3) {
                                        if (status != 2) {
                                            String comm = " Несоответствие - услуга телефоние исходящая";
                                            MainFrame.insertRow(phone, comm);
                                        }
                                    } else {
                                        if (status != invStatus) {
                                            String comm = " Несоответствие - услуга телефоние исходящая";
                                            MainFrame.insertRow(phone, comm);
                                        }
                                    }
                                } else {
                                    String comm = " Несоответствие - услуга телефоние исходящая 2";
                                    MainFrame.insertRow(phone, comm);
                                }
                                status = -1;
                                break;
                            /* Incoming telephony */
                            case 3:
                                status = worker.parseIncomingTelephony(subsData);
                                if (status != -1) {
                                    /* if telephony has blocked or turned off status then we consider it as status=2 */
                                    int invStatus = rs2.getInt("sstat_id");
                                    if (invStatus == 2 || invStatus == 3) {
                                        if (status != 2) {
                                            String comm = " Несоответствие - услуга телефоние входящая";
                                            MainFrame.insertRow(phone, comm);
                                        }
                                    } else {
                                        if (status != invStatus) {
                                            String comm = " Несоответствие - услуга телефоние входящая";
                                            MainFrame.insertRow(phone, comm);
                                        }
                                    }
                                } else {
                                    String comm = " Несоответствие - услуга телефоние входящая";
                                    MainFrame.insertRow(phone, comm);
                                }
                                status = -1;
                                break;
                        }
                    }
                    st.close();
                } else {
                    MainFrame.addLog("Error, cant get data for " + phone + "\n" + worker.lastCommandErrorMsg);
                }
                /* increment subs counter */
                MainFrame.showSubsProcessedResult();
                subsProcessed++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
//                close connections to database and hlr
                worker.logoutFromHLR();
                conn.close();
                long EndTime = new Date().getTime() / 1000;
                MainFrame.addLog("Thread " + Thread.currentThread().getName() + " finished; Execution time (sec) : " + (EndTime - StartTime) + " processed : " + subsProcessed);
            } catch (Exception e) {
            }

        }

    }
}
