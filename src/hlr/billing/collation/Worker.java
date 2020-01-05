/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hlr.billing.collation;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Date;
import javax.swing.JTextArea;

/**
 *
 * @author vazhenin
 */
public class Worker {

    final String hlrlogin = "hlrbilling";
    final String hlrpwd = "Passwd321";
    final String hlr_host = "10.1.5.88";
    final int hlr_host_port = 7778;
    Socket sock;
    String lastCommandErrorMsg;
    private final long msgSendsleepTime = 10;
    private final int maxReadAttempts = 100;
    /* answer variants */
    private String resultSuccess = "RETCODE = 0";
    private BufferedOutputStream connOutStream;

    public BufferedOutputStream getConnOutStream() {
        return connOutStream;
    }
    private InputStream connInStream;
    DButilities db;

    public Worker() {
    }

    public Worker(String ip, int port) {
    }

    /**
     * make establishConnection to HLR database / Returns PrintWriter object
     */
    void establishConnection() {
        try {
            sock = new Socket(hlr_host, hlr_host_port);
            connOutStream = new BufferedOutputStream(sock.getOutputStream());
            connInStream = sock.getInputStream();
//            System.out.println(Thread.currentThread().getName() + " " + connInStream.hashCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String parseMessage(InputStream is) {
        String result = "no answer", line = null;
        String endSign = "---    END";
        try {
            BufferedInputStream bis = new BufferedInputStream(connInStream);

            int ReadAttempts = 0;
            while (bis.available() == 0) {
                ReadAttempts++;
                if (ReadAttempts == maxReadAttempts) {
                    break;
                }
                Thread.sleep(msgSendsleepTime);
            }

            if (bis.available() > 0) {
                while (bis.available() != 0) {
                    byte[] b = new byte[bis.available()];
                    bis.read(b);
                    result += new String(b);
                    if (!result.contains(endSign)) {
                        if (bis.available() == 0) {
                            ReadAttempts = 0;
                            while (bis.available() == 0) {
                                ReadAttempts++;
                                if (ReadAttempts == maxReadAttempts) {
//                                    System.out.println("Didn't reach end of data for imsi" + parseIMSI(result));
//                                    bis.reset();
                                    break;
                                }
//                                System.out.println("Sleeping....");
                                Thread.sleep(msgSendsleepTime);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Connect to HLR database
     */
    public int loginToHLR() {
        int result = -1;
        String answer;
        try {
            answer = sendMessage("LGI:OPNAME=\"" + hlrlogin + "\",PWD=\"" + hlrpwd + "\";");
            if (ifOperationSuccessful(answer)) {
                result = 0;
            } else {
                lastCommandErrorMsg = answer;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Logout from HLR database
     */
    public int logoutFromHLR() {
        int result = -1;
        String answer;
        try {
            answer = sendMessage("LGO:;");
            if (ifOperationSuccessful(answer)) {
                result = 0;
            } else {
                lastCommandErrorMsg = answer;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Get subscriber data by IMSI
     */
    public String getSubsDataByIMSI(String imsi) {
        String result = null;
        String answer;
        String dataStep = "Operation is successful";
        try {
            answer = sendMessage("LST SUB: IMSI=\"" + imsi + "\", DETAIL=TRUE;");
            if (ifOperationSuccessful(answer)) {
                result = answer.substring(answer.indexOf(dataStep) + dataStep.length(), answer.length());;
            } else {
                lastCommandErrorMsg = answer;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Get subscriber data by MSISDN
     */
    public String getSubsDataByMSISDN(String msisdn) {
        String result = null;
        String answer;
        String dataStep = "Operation is successful";
        try {
            answer = sendMessage("LST SUB: ISDN=\"" + msisdn + "\", DETAIL=TRUE;");
            if (ifOperationSuccessful(answer)) {
                result = answer.substring(answer.indexOf(dataStep) + dataStep.length(), answer.length());
            } else {
                lastCommandErrorMsg = answer;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
    
    public boolean ifOperationSuccessful(String result) {
        return result.indexOf(resultSuccess) != -1;
    }

    /**
     * parse IMSI from subscriber answer message
     */
    public String parseIMSI(String subsData) {
        String imsi = null;
        try {
            imsi = subsData.substring(subsData.indexOf("IMSI"), subsData.length());
            imsi = imsi.substring(1, imsi.indexOf("\n"));
            imsi = imsi.substring(imsi.indexOf("=") + 2, imsi.length()).trim();
        } catch (Exception e) {
            System.out.println(subsData);
            e.printStackTrace();
        }

        return imsi;
    }

    /**
     * parse ISDN from subscriber answer message
     */
    public String parseISDN(String subsData) {
        String isdn = null;
        try {
            isdn = subsData.substring(subsData.indexOf("ISDN"), subsData.length());
            isdn = isdn.substring(1, isdn.indexOf("\n"));
            isdn = isdn.substring(isdn.indexOf("=") + 2, isdn.length()).trim();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return isdn;
    }

    /**
     * parse status of Outgoing Telephony from subscriber answer message
     */
    public int parseOutgoingTelephony(String subsData) {
        int Result = -1;
        try {
            if (subsData.indexOf("ODBOC = NOBOC") != -1) {
                Result = 1; /* active */

            }

            if (subsData.indexOf("ODBOC = BAOC") != -1) {
                Result = 2; /* blocked or turned off */

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result;
    }

    /**
     * parse status of Incoming Telephony from subscriber answer message
     */
    public int parseIncomingTelephony(String subsData) {
        int Result = -1;
        try {
            if (subsData.indexOf("ODBIC = NOBIC") != -1) {
                Result = 1; /* active */

            }

            if (subsData.indexOf("ODBIC = BAIC") != -1) {
                Result = 2; /* blocked or turned off */

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result;
    }

    /**
     * parse charge type PrePaid or PostPaid for Packet Data
     */
    public int parsePDChargeType(String subsData) {
        int Result = -1;
        try {
            if (subsData.indexOf("CHARGE_GLOBAL = NORMAL") != -1) {
                Result = 2; /* postpaid Packet Data */

            }

            if (subsData.indexOf("CHARGE_GLOBAL = PREPAID") != -1) {
                Result = 1; /* prepaid Packet Data */

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result;
    }

    /**
     * parse charge type PrePaid or PostPaid for Voice - Telephony
     */
    public int parseVoiceChargeType(String subsData) {
        int Result = 2; /* postpaid by default */

        try {
            if (subsData.indexOf("O-CSI") != -1 && subsData.indexOf("T-CSI") != -1) {
                Result = 1; // prepaid Voice 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result;
    }

    synchronized String sendMessage(String msgText) {
        String res = null;
        try {
            connOutStream.write(msgText.getBytes());
            connOutStream.flush();
            res = parseMessage(connInStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }
}
