package npble.nopointer.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.text.TextUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.dfu.internal.scanner.BootloaderScanner;
import npble.nopointer.exception.NpBleUUIDNullException;
import npble.nopointer.log.NpBleLog;

/**
 * Created by nopointer on 2017/6/12.
 */

public class BleUtil {


    /**
     * mac地址的正则匹配表达式
     */
    private static final String strMacRule = "^[A-F0-9]{2}(:[A-F0-9]{2}){5}$";


    public static String debug(byte[] data) {
        StringBuilder sb = new StringBuilder().append("[");
        for (byte b : data) {
            sb.append(String.format("%02X,", (b & 0xff)));
        }
        return sb.deleteCharAt(sb.length() - 1).append("]").toString();
    }

    public static void debug(String tag, byte[] data) {
        NpBleLog.log(tag + debug(data));
    }

    /***
     *字节数组转成16进制数据
     * @param data
     * @return
     */
    public static String byte2HexStr(byte[] data) {
        if (data == null || data.length < 1)
            return " ";
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X", (b & 0xff)));
        }
        return sb.toString();
    }

    /**
     * 字节数组转成16进制数据
     *
     * @param data
     * @param flag 中间的分隔符
     * @return
     */
    public static String byte2HexStr(byte[] data, String flag) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X", (b & 0xff))).append(flag);
        }
        if (sb.length() > 1) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * 16进制字符串转字节数组
     *
     * @param hexString
     * @return
     */
    public static byte[] hexStr2Byte(String hexString) {
        int len = hexString.length();
        byte[] result = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            result[i / 2] = (byte) (Integer.parseInt(hexString.substring(i, i + 2), 16) + 0);
        }
        return result;
    }

    //高位在前
    public static byte[] int2ByteArrLR(int number, int arrayLen) {
        byte[] result = new byte[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            long tmp1 = number & (255 << (arrayLen - 1 - i));
            tmp1 >>= ((arrayLen - 1 - i) * 8);
            tmp1 &= 0xFF;
            result[i] = (byte) tmp1;
        }
        return result;
    }

    //低位在前
    public static byte[] int2ByteArrRL(int number, int arrayLen) {
        byte tmp[] = int2ByteArrLR(number, arrayLen);
        byte[] result = new byte[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            result[arrayLen - 1 - i] = tmp[i];
        }
        return result;
    }

    public static int byte2IntLR(byte... arr) {
        int result = 0, len = arr.length;
        for (int i = 0; i < len; i++) {
            result |= ((arr[i] & 0xff) << (len - i - 1) * 8);
        }
        return result;
    }


    public static int byte2IntRL(byte... arr) {
        int result = 0, len = arr.length;
        for (int i = 0; i < len; i++) {
            result |= ((arr[i] & 0xff) << (i) * 8);
        }
        return result;
    }

    public static short byte2ShortLR(byte... arr) {
        short result = 0, len = (short) arr.length;
        for (int i = 0; i < len; i++) {
            result |= ((arr[i] & 0xff) << (len - i - 1) * 8);
        }
        return result;
    }

    public static short byte2ShortRL(byte... arr) {
        short result = 0, len = (short) arr.length;
        for (int i = 0; i < len; i++) {
            result |= ((arr[i] & 0xff) << (i) * 8);
        }
        return result;
    }

    public static void main(String[] args) {
//        System.out.println(BleUtil.byte2HexStr(int2ByteArrRL(01, 2)));
//        ACED000574000120
//        ACED0005740000
//        ACED000570
        encryption("");
        Decrypt(null);
    }

    private static String Decrypt(Object object) {
        byte[] data = BleUtil.hexStr2Byte("ACED0005740000");
        ByteArrayInputStream ins = new ByteArrayInputStream(data);
        System.out.println(new String(data));
        return "";
    }

    private static String encryption(Object object) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            System.out.println(byte2HexStr(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static int byteStr2IntLR(String hexStr) {
        return byte2IntLR(hexStr2Byte(hexStr));
    }

    public static int byteStr2IntRL(String hexStr) {
        return byte2IntRL(hexStr2Byte(hexStr));
    }


    /**
     * 将一个二进制数据，转化为整数
     */
    public static int bin2Int(String binValue) {
        char[] bits = binValue.replace(" ", "").toCharArray();
        int resInt = 0;
        for (char c : bits)
            resInt = resInt * 2 + (c == '1' ? 1 : 0);
        return resInt;
    }

    public static String week2Bit(boolean[] week) {
        String result = "";
        for (int i = 6; i >= 0; i--) {
            if (week[i])
                result += "1";
            else
                result += "0";
        }
        return result;
    }

    public static boolean[] bit2Week(String bit) {
        boolean result[] = new boolean[7];
        for (int i = 0; i < 7; i++)
            result[6 - i] = bit.substring(i, i + 1).equals("1");
        return result;
    }

    /**
     * 将一个十进制的数转成8位二进制数
     *
     * @param intValue
     * @return
     */
    public static String int2Bit(int intValue) {
        return String.format("%08d", Integer.valueOf(Integer.toBinaryString(intValue)));
    }


    /**
     * mac地址自增 nordic的DFU模式下 设备的mac地址会发生变化
     *
     * @param deviceAddress
     * @return
     */
    public static String getIncrementedAddress(final String deviceAddress) {
        final String firstBytes = deviceAddress.substring(0, 15);
        final String lastByte = deviceAddress.substring(15); // assuming that the device address is correct
        final String lastByteIncremented = String.format("%02X", (Integer.valueOf(lastByte, 16) + BootloaderScanner.ADDRESS_DIFF) & 0xFF);
        return firstBytes + lastByteIncremented;
    }


    /**
     * 反转byte数组
     *
     * @param a
     * @return
     */
    public static byte[] reverse(byte[] a) {
        if (a == null)
            return null;

        int p1 = 0, p2 = a.length;
        byte[] result = new byte[p2];

        while (--p2 >= 0) {
            result[p2] = a[p1++];
        }
        return result;
    }


    /**
     * 反转byte数组中的某一段
     *
     * @param arr
     * @param begin
     * @param end
     * @return
     */
    public static byte[] reverse(byte[] arr, int begin, int end) {

        while (begin < end) {
            byte temp = arr[end];
            arr[end] = arr[begin];
            arr[begin] = temp;
            begin++;
            end--;
        }

        return arr;
    }


    /**
     * 判断设备是否在连接列表里面
     *
     * @param mac
     * @param context
     * @return
     */
    public static BluetoothDevice isInConnList(String mac, Context context) {
        //打印一下连接的蓝牙设备
        List<BluetoothDevice> devices = connDeviceList(context);
        if (devices == null || devices.size() < 1) return null;
        for (BluetoothDevice device : devices) {
            if (device.getAddress().equalsIgnoreCase(mac)) {
                return device;
            }
        }
        return null;
    }


    /**
     * 获取设备的连接列表
     *
     * @param context
     * @return
     */
    public static List<BluetoothDevice> connDeviceList(Context context) {
        NpBleLog.log("读取当前系统连接的蓝牙设备");
        if (context == null) {
            return null;
        }
        final BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager == null || manager.getAdapter() == null || !manager.getAdapter().isEnabled()) {
            return null;
        }
        final List<BluetoothDevice> devices = manager.getConnectedDevices(BluetoothProfile.GATT);
        for (BluetoothDevice device : devices) {
            NpBleLog.log("debug==>device：===>" + device.getAddress() + "==" + device.getName());
        }
        return devices;
    }


    /**
     * 获取指定的服务
     *
     * @param bluetoothGatt
     * @param U_service
     * @return
     * @throws NpBleUUIDNullException
     */
    public static BluetoothGattService getService(BluetoothGatt bluetoothGatt, UUID U_service) throws NpBleUUIDNullException {
        if (bluetoothGatt == null) {
            throw new NpBleUUIDNullException(String.format("not find this bluetoothGatt", U_service.toString()));
        }
        BluetoothGattService result = bluetoothGatt.getService(U_service);
        if (result == null) {
            throw new NpBleUUIDNullException(String.format("not find this uuid %s for BluetoothGattService", U_service.toString()));
        }
        return result;
    }

    /**
     * 获取指定的特征
     *
     * @param bluetoothGatt
     * @param serviceUUId
     * @param characteristicUUId
     * @return
     * @throws NpBleUUIDNullException
     */
    public static BluetoothGattCharacteristic getCharacteristic(BluetoothGatt bluetoothGatt, UUID serviceUUId, UUID characteristicUUId) throws NpBleUUIDNullException {
        return getCharacteristic(getService(bluetoothGatt, serviceUUId), characteristicUUId);
    }


    /**
     * 获取指定的特征
     *
     * @param service
     * @param U_chara
     * @return
     * @throws NpBleUUIDNullException
     */
    public static BluetoothGattCharacteristic getCharacteristic(BluetoothGattService service, UUID U_chara) throws NpBleUUIDNullException {
        BluetoothGattCharacteristic result = service.getCharacteristic(U_chara);
        if (result == null) {
            throw new NpBleUUIDNullException(String.format("not find this uuid %s for BluetoothGattCharacteristic please check service  or charateristic uuid", U_chara.toString()));
        }
        return result;
    }

    /**
     * 获取指定的描述符
     *
     * @param characteristic
     * @param U_descriptor
     * @return
     * @throws NpBleUUIDNullException
     */
    public static BluetoothGattDescriptor getDescriptor(BluetoothGattCharacteristic characteristic, UUID U_descriptor) throws NpBleUUIDNullException {
        BluetoothGattDescriptor result = characteristic.getDescriptor(U_descriptor);
        if (result == null) {
            throw new NpBleUUIDNullException(String.format("not find this uuid %s for BluetoothGattCharacteristic please check service  or charateristic or descriptor uuid", U_descriptor.toString()));
        }
        return result;
    }


    /**
     * 蓝牙开关是否是打开的
     *
     * @return
     */
    public static boolean isBLeEnabled() {
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }


    /**
     * 判断是否是正确的蓝牙mac地址
     *
     * @param mac
     * @return
     */
    public static boolean isRightBleMacAddress(String mac) {
        if (TextUtils.isEmpty(mac) || !mac.matches(strMacRule)) {
            return false;
        }
        return true;
    }

    /**
     * g根据mac地址获取蓝牙设备
     *
     * @param mac
     * @return
     */
    public static BluetoothDevice getBluetoothDevice(String mac) {
        return BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac);
    }






}
