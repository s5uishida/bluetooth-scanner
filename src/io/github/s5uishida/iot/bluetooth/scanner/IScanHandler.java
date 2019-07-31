package io.github.s5uishida.iot.bluetooth.scanner;

import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;

/*
 * @author s5uishida
 *
 */
public interface IScanHandler {
	void handle(BluetoothDevice device, ScanData data);
}
