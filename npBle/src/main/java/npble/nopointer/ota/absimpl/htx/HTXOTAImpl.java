package npble.nopointer.ota.absimpl.htx;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.example.otalib.boads.Constant;
import com.example.otalib.boads.Utils;
import com.example.otalib.boads.WorkOnBoads;

import java.io.IOException;
import java.util.UUID;

import npble.nopointer.ble.conn.NpBleAbsConnManager;
import npble.nopointer.exception.NpBleUUIDNullException;
import npble.nopointer.log.NpBleLog;
import npble.nopointer.ota.NpOtaErrCode;
import npble.nopointer.ota.callback.NpOtaCallback;
import npble.nopointer.util.BleUtil;


/**
 * 沙雕汉天下的ota demo 写的太辣鸡了 只能自己写一个了
 */
public class HTXOTAImpl extends NpBleAbsConnManager implements HTXBleCfg {


    public static final int MSG_OTA_RESEPONSE = 0x09;


    public static final int MSG_DISCONNECT_BLE = 0x10;

    private WorkOnBoads workOnBoads;
    private String filePath = null;

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    private String mac;
    private Context context;

    public void setMac(String mac) {
        this.mac = mac;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    private NpOtaCallback otaCallback;


    public void setOtaCallback(NpOtaCallback otaCallback) {
        this.otaCallback = otaCallback;
    }

    //是否是ota成功了
    private boolean isOTASuccess = false;

    //文件总进度
    private int fileTotalSize = 0;


    public HTXOTAImpl(Context context) {
        super(context);
        isOTASuccess = false;
        setMustUUID(UUID_OTA_RX_CMD);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg == null) return;
            NpBleLog.log(msg.toString());

            switch (msg.arg1) {

                //文件响应
                case MSG_OTA_RESEPONSE:
                    if (msg.obj != null) {
                        workOnBoads.resetTarget();
                    }
                    break;

                case MSG_DISCONNECT_BLE:
                    if (msg.obj != null) {
                        disConnectDevice();
                    }
                    break;
                //1000
                case Constant.MSG_ARG1_SEND_OTA_CMD: {
                    int pos1 = 0;
                    int len3 = msg.arg2;
                    int tmp1 = len3 % 20;
                    byte[] sendcmd = (byte[]) msg.obj;

                    for (int i = 0; i < len3 / 20; i++) {
                        byte[] packet_data = new byte[20];
                        System.arraycopy(sendcmd, pos1, packet_data, 0, 20);
                        if (isConnected()) {
                            boolean result = writeTxCmd(packet_data);
                            if (!result) {
                                NpBleLog.log("writeCharacteristic() TX CMD failed!!!");
                                return;
                            }
                            pos1 = pos1 + 20;
                        } else {
                            if (otaCallback != null) {
                                otaCallback.onFailure(NpOtaErrCode.LOST_CONN, "lost conn");
                            }
                            return;
                        }
                    }

                    if (tmp1 != 0) {
                        byte[] packet_data = new byte[tmp1];
                        System.arraycopy(sendcmd, pos1, packet_data, 0, tmp1);
                        if (isConnected()) {
                            NpBleLog.log("cmd data:" + Utils.bytesToHexString(packet_data));

                            boolean result = writeTxCmd(packet_data);
                            if (!result) {
                                NpBleLog.log("writeCharacteristic() TX CMD failed!!!");
                                return;
                            }
                            if (BleUtil.byte2HexStr(packet_data).equals("00000000")) {
                                NpBleLog.log("复位标识");
                            }
                            NpBleLog.log(packet_data.toString());
                        } else {
                            if (otaCallback != null) {
                                otaCallback.onFailure(NpOtaErrCode.LOST_CONN, "lost conn");
                            }
                            return;
                        }
                    }
                }
                break;

                //1004
                case Constant.MSG_ARG1_SEND_OTA_DATA: {
                    int pos = 0;
                    int len = msg.arg2;
                    int tmp = len % 20;
                    byte[] senddat = (byte[]) msg.obj;

                    for (int i = 0; i < len / 20; i++) {
                        byte[] packet_data = new byte[20];
                        System.arraycopy(senddat, pos, packet_data, 0, 20);

                        if (isConnected()) {
                            NpBleLog.log("ota send lenth:" + packet_data.length);
                            boolean result = writeTxData(packet_data);
                            if (!result) {
                                NpBleLog.log("writeCharacteristic() DATA failed!!!");
                                return;
                            }
                            pos = pos + 20;
                            NpBleLog.log("ota pos:" + pos + " / " + len);

                        } else {
                            if (otaCallback != null) {
                                otaCallback.onFailure(NpOtaErrCode.LOST_CONN, "lost conn");
                            }
                            return;
                        }
                    }
                    if (tmp != 0) {
                        byte[] packet_data = new byte[tmp];
                        System.arraycopy(senddat, pos, packet_data, 0, tmp);
                        if (isConnected()) {
//                            NpBleLog.log("send data:" + Utils.bytesToHexString(packet_data));
                            boolean result = writeTxData(packet_data);
                            if (!result) {
                                NpBleLog.log("writeCharacteristic() DATA failed!!!");
                                return;
                            }
                        } else {
                            if (otaCallback != null) {
                                otaCallback.onFailure(NpOtaErrCode.LOST_CONN, "lost conn");
                            }
                            return;
                        }
                    }
                }
                break;

                case Constant.MSG_ARG1_PROGRESS_BAR_MAX: {
                    int len1 = msg.arg2;
//                    NpBleLog.log("len==>1 " + len1);
                    fileTotalSize = len1;
                }
                break;

                case Constant.MSG_ARG1_PROGRESS_BAR_UPDATA: {
                    int currentValue = msg.arg2;
                    float progress = (currentValue * 100) / fileTotalSize;
//                    NpBleLog.log("len==>2 " + currentValue + "/" + fileTotalSize);

                    if (progress >= 100) {
                        isOTASuccess = true;
                        NpBleLog.log("OTA 成功了");
                    } else {
                        if (otaCallback != null) {
                            otaCallback.onProgress((int) progress);
                        }
                    }
                }
                break;
            }
        }
    };

    @Override
    public void onFinishTaskAfterConn() {
        loadFile();
    }

    @Override
    public void onDataReceive(byte[] data, UUID uuid) {
        NpBleLog.log("Receive:" + uuid.toString() + "///" + BleUtil.byte2HexStr(data));
        if (uuid.equals(UUID_OTA_RX_CMD)) {
            workOnBoads.setBluetoothNotifyData(data, Constant.CMDCHARC);
        } else if (uuid.equals(UUID_OTA_RX_DATA)) {
            workOnBoads.setBluetoothNotifyData(data, Constant.DATACHARC);
        }
    }


    @Override
    protected void onBeforeWriteData(UUID uuid, byte[] data) {

    }

    @Override
    public void loadCfg() {
        try {
            setNotificationCallback(UUID_OTA_SERVICE, UUID_OTA_RX_CMD);
            enableNotifications(UUID_OTA_SERVICE, UUID_OTA_RX_CMD);

            setNotificationCallback(UUID_OTA_SERVICE, UUID_OTA_RX_DATA);
            enableNotifications(UUID_OTA_SERVICE, UUID_OTA_RX_DATA);
        } catch (NpBleUUIDNullException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onConnException() {
        if (isOTASuccess) {
            if (otaCallback != null) {
                otaCallback.onSuccess();
            }
        } else {
            if (otaCallback != null) {
                otaCallback.onFailure(NpOtaErrCode.LOST_CONN, "exception");
            }
        }
    }

    @Override
    protected void onDataWriteSuccess(UUID uuid, byte[] data) {

    }

    @Override
    protected void onDataWriteFail(UUID uuid, byte[] data, int status) {

    }

    private Thread mTransThread = null;

    private void loadFile() {
        if (workOnBoads == null) {
            workOnBoads = new WorkOnBoads(context, handler);
        }

        mTransThread = new Thread(new Runnable() {
            @Override
            public void run() {
                workOnBoads.setEncrypt(false);

                byte[] tmp_read;
                Utils op = new Utils();
                try {
                    //do_work_on_boads.app_buf = op.readSDFile(file_path);
                    tmp_read = op.readSDFile(filePath);
                    NpBleLog.log("tmp_read 固件文件的大小==>" + tmp_read.length);

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return;
                }
                int response = workOnBoads.loadBinary(tmp_read, Constant.APPTYPE);
                sendFileRseponse(response);

                Message msg = Message.obtain();
                msg.arg1 = MSG_DISCONNECT_BLE;
                handler.sendMessage(msg);
            }
        });
        mTransThread.start();
    }

    private void sendFileRseponse(int response) {
        String detail;
        switch (response) {
            case Constant.NORESPONSEERROR:
                detail = "OTA is not response!";
                break;
            case Constant.INVALIDPARAMETERERROR:
                detail = "Send parameter error!";
                break;
            case Constant.FILETOBIGERROR:
                detail = "Too large a file to send!";
                break;
            case Constant.LOADBINARYFILEERROR:
                detail = "Send load binary file error!";
                break;
            case Constant.EXECFORMATERROR:
                detail = "Error sending upgrade package format!";
                break;
            case Constant.USERADDRERROR:
                detail = "User Addr error !";
                break;
            case 0:
                detail = "OTA has been successful!";
                break;
            default:
                detail = "返回值是：" + response;
                break;
        }

        NpBleLog.log("结束了传输！！！response:" + response + " /// detail:" + detail);
//        ycBleLogUtils.e("detail:" + detail);
        Message msg = handler.obtainMessage();
        msg.arg1 = MSG_OTA_RESEPONSE;
        msg.obj = detail;
        handler.sendMessage(msg);
    }


    public void startOTA() {
        connDevice(mac);
    }


    public void stopOTA() {
        disConnectDevice();
    }

    private boolean writeTxCmd(byte data[]) {
        try {
            writeCharacteristicWithOutResponse(UUID_OTA_SERVICE, UUID_OTA_TX_CMD, data);
            return true;
        } catch (NpBleUUIDNullException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean writeTxData(byte data[]) {
        try {
            writeCharacteristicWithOutResponse(UUID_OTA_SERVICE, UUID_OTA_TX_DATA, data);
            return true;
        } catch (NpBleUUIDNullException e) {
            e.printStackTrace();
            return false;
        }
    }
}
