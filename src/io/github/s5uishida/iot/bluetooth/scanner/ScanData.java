package io.github.s5uishida.iot.bluetooth.scanner;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/*
 * @author s5uishida
 *
 */
public class ScanData {
	protected static final int RSSI_UNSET = Integer.MAX_VALUE;
	protected static final int TXPOWER_UNSET = Integer.MAX_VALUE;

	private static final String dateFormat = "yyyy-MM-dd HH:mm:ss.SSS";
	private static final SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);

	private final String adapterDeviceName;
	private final String address;
	private final String name;
	private final int rssi;
	private final int txPower;
	private final Date date;
	private final String logPrefix;

	public ScanData(String adapterDeviceName, String address, String name, int rssi, int txPower, Date date) {
		this.adapterDeviceName = Objects.requireNonNull(adapterDeviceName);
		this.address = Objects.requireNonNull(address);
		this.name = name;
		this.rssi = rssi;
		this.txPower = txPower;
		this.date = date;
		this.logPrefix = "[" + adapterDeviceName + "] " + address + " ";
	}

	public String getAdapterDeviceName() {
		return adapterDeviceName;
	}

	public String getAddress() {
		return address;
	}

	public String getName() throws NoSuchFieldException {
		if (name == null) {
			throw new NoSuchFieldException(logPrefix + "Name not found.");
		}
		return name;
	}

	public int getRssi() throws NoSuchFieldException {
		if (rssi == RSSI_UNSET) {
			throw new NoSuchFieldException(logPrefix + "RSSI not found.");
		}
		return rssi;
	}

	public int getTxPower() throws NoSuchFieldException {
		if (txPower == TXPOWER_UNSET) {
			throw new NoSuchFieldException(logPrefix + "TxPower not found.");
		}
		return txPower;
	}

	public Date getDate() {
		return date;
	}

	public String getDateString() {
		return sdf.format(date);
	}

	@Override
	public String toString() {
		String rssiString = null;
		String txPowerString = null;

		if (rssi != RSSI_UNSET) {
			rssiString = String.valueOf(rssi);
		}
		if (txPower != TXPOWER_UNSET) {
			txPowerString = String.valueOf(txPower);
		}

		return logPrefix +
				"name:" + name +
				" rssi:" + rssiString +
				" txPower:" + txPowerString +
				" date:" + getDateString();
	}
}
