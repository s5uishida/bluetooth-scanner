package io.github.s5uishida.iot.bluetooth.scanner;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.bluez.Device1;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezNotReadyException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hypfvieh.DbusHelper;
import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.DiscoveryFilter;
import com.github.hypfvieh.bluetooth.DiscoveryTransport;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothAdapter;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;

/*
 * @author s5uishida
 *
 */
public class ScanProcess {
	private static final Logger LOG = LoggerFactory.getLogger(ScanProcess.class);

	protected final String adapterDeviceName;
	protected final IScanHandler scanHandler;
	protected final String logPrefix;

	private final ScanProcessThread scanProcessThread;

	protected DeviceManager manager;
	protected BluetoothAdapter adapter;

	protected final Map<String, BluetoothDevice> deviceMap = new HashMap<String, BluetoothDevice>();

	public ScanProcess(String adapterDeviceName, IScanHandler scanHandler) {
		this(adapterDeviceName, scanHandler, null,
				ScanProcessThread.DEFAULT_SCAN_PERIOD_MILLIS, ScanProcessThread.DEFAULT_SCAN_INTERVAL_MILLIS);
	}

	public ScanProcess(String adapterDeviceName, IScanHandler scanHandler, Map<DiscoveryFilter, Object> filter) {
		this(adapterDeviceName, scanHandler, filter,
				ScanProcessThread.DEFAULT_SCAN_PERIOD_MILLIS, ScanProcessThread.DEFAULT_SCAN_INTERVAL_MILLIS);
	}

	public ScanProcess(String adapterDeviceName, IScanHandler scanHandler, Map<DiscoveryFilter, Object> filter,
			int scanPeriodMillis, int scanIntervalMillis) {
		this.adapterDeviceName = Objects.requireNonNull(adapterDeviceName);
		this.scanHandler = Objects.requireNonNull(scanHandler);
		this.logPrefix = "[" + this.adapterDeviceName + "] ";

		try {
			manager = DeviceManager.getInstance();
		} catch (IllegalStateException e) {
			LOG.info("caught - {}", e.toString());
			try {
				manager = DeviceManager.createInstance(false);
			} catch (DBusException e1) {
				throw new IllegalStateException(e1);
			}
		}

		try {
			if (filter != null && filter.size() > 0) {
				manager.setScanFilter(filter);
			}
		} catch (BluezInvalidArgumentsException | BluezNotReadyException | BluezNotSupportedException
				| BluezFailedException e) {
			throw new IllegalStateException(e);
		}

		try {
			manager.registerPropertyHandler(new ScanPropertiesChangedHandler(this));
			List<BluetoothAdapter> adapters = manager.getAdapters();
			for (BluetoothAdapter adapter : adapters) {
				LOG.info("[{}] found.", adapter.getDeviceName());
				if (adapter.getDeviceName().equals(this.adapterDeviceName)) {
					this.adapter = adapter;
				}
			}
			if (adapter == null) {
				throw new IllegalStateException(logPrefix + "not found.");
			}

			LOG.info(logPrefix + "selected for scanning.");

			if (!adapter.isPowered()) {
				adapter.setPowered(true);
				LOG.info(logPrefix + "power on.");
			}
		} catch (DBusException e) {
			throw new IllegalStateException(e);
		}

		scanProcessThread = new ScanProcessThread(scanPeriodMillis, scanIntervalMillis);
	}

	public DeviceManager getDeviceManager() {
		return manager;
	}

	public BluetoothAdapter getAdapter() {
		return adapter;
	}

	public String getAdapterDeviceName() {
		return adapterDeviceName;
	}

	public void setScanPeriod(int scanPeriodMillis) {
		scanProcessThread.setScanPeriod(scanPeriodMillis);
	}

	public int getScanPeriod() {
		return scanProcessThread.getScanPeriod();
	}

	public void setScanInterval(int scanIntervalMillis) {
		scanProcessThread.setScanInterval(scanIntervalMillis);
	}

	public int getScanInterval() {
		return scanProcessThread.getScanInterval();
	}

	public Map<String, BluetoothDevice> getDeviceMap() {
		return deviceMap;
	}

	public void start() {
		scanProcessThread.setDaemon(true);
		scanProcessThread.start();
	}

	public void stop() {
		scanProcessThread.stopThread();
	}

	public boolean isAlive() {
		return scanProcessThread.isAlive();
	}

	class ScanProcessThread extends Thread {
		private final Logger LOG = LoggerFactory.getLogger(ScanProcessThread.class);

		private static final int DEFAULT_SCAN_PERIOD_MILLIS = 5000;
		private static final int MIN_SCAN_PERIOD_MILLIS = 100;
		private static final int DEFAULT_SCAN_INTERVAL_MILLIS = 5000;
		private static final int MIN_SCAN_INTERVAL_MILLIS = 100;

		private volatile int scanPeriodMillis = DEFAULT_SCAN_PERIOD_MILLIS;
		private volatile int scanIntervalMillis = DEFAULT_SCAN_INTERVAL_MILLIS;
		private volatile boolean running = true;

		public ScanProcessThread() {
			this(DEFAULT_SCAN_PERIOD_MILLIS, DEFAULT_SCAN_INTERVAL_MILLIS);
		}

		public ScanProcessThread(int scanPeriodMillis, int scanIntervalMillis) {
			setScanPeriod(scanPeriodMillis);
			setScanInterval(scanIntervalMillis);
		}

		public void setScanPeriod(int scanPeriodMillis) {
			if (scanPeriodMillis < MIN_SCAN_PERIOD_MILLIS) {
				this.scanPeriodMillis = MIN_SCAN_PERIOD_MILLIS;
				LOG.info(logPrefix + "{} msec specified for Scanning is shorter than {} msec, so change to {} msec.",
						scanPeriodMillis, MIN_SCAN_PERIOD_MILLIS, MIN_SCAN_PERIOD_MILLIS);
			} else {
				this.scanPeriodMillis = scanPeriodMillis;
			}
			LOG.info(logPrefix + "{} msec for Scanning is set.", this.scanPeriodMillis);
		}

		public int getScanPeriod() {
			return scanPeriodMillis;
		}

		public void setScanInterval(int scanIntervalMillis) {
			if (scanIntervalMillis < MIN_SCAN_INTERVAL_MILLIS) {
				this.scanIntervalMillis = MIN_SCAN_INTERVAL_MILLIS;
				LOG.info(logPrefix + "{} msec specified for Scanning interval is shorter than {} msec, so change to {} msec.",
						scanIntervalMillis, MIN_SCAN_INTERVAL_MILLIS, MIN_SCAN_INTERVAL_MILLIS);
			} else {
				this.scanIntervalMillis = scanIntervalMillis;
			}
			LOG.info(logPrefix + "{} msec for Scanning interval is set.", this.scanIntervalMillis);
		}

		public int getScanInterval() {
			return scanIntervalMillis;
		}

		public void stopThread() {
			running = false;
			LOG.info(logPrefix + "scanning advertising signals stopped.");
		}

		@Override
		public void run() {
			LOG.info(logPrefix + "scanning advertising signals started.");
			try {
				while(running) {
					try {
						adapter.startDiscovery();
					} catch (DBusExecutionException e) {
						LOG.warn("caught - {}", e.toString(), e);
					}
					Thread.sleep(scanPeriodMillis);
					try {
						adapter.stopDiscovery();
					} catch (DBusExecutionException e) {
						LOG.warn("caught - {}", e.toString(), e);
					}
					Thread.sleep(scanIntervalMillis);
				}
			} catch (InterruptedException e) {
				LOG.warn("caught - {}", e.toString(), e);
			}
		}
	}

	/******************************************************************************************************************
	 * Sample main
	 ******************************************************************************************************************/
	public static void main(String[] args) throws IOException, InterruptedException {
		Map<DiscoveryFilter, Object> filter = new HashMap<DiscoveryFilter, Object>();
		filter.put(DiscoveryFilter.Transport, DiscoveryTransport.LE);

		ScanProcess scanProcess = new ScanProcess("hci0", new MyScanHandler(), filter);
		scanProcess.start();
	}
}

class ScanPropertiesChangedHandler extends AbstractPropertiesChangedHandler {
	private static final Logger LOG = LoggerFactory.getLogger(ScanPropertiesChangedHandler.class);

	private final ScanProcess scanProcess;
	private final String prefix;

	private final BlockingQueue<ScanPropertiesChangedData> queue = new LinkedBlockingQueue<ScanPropertiesChangedData>();
	private final Thread scanPropertiesChangedThread;

	public ScanPropertiesChangedHandler(ScanProcess scanProcess) {
		this.scanProcess = scanProcess;
		prefix = "/org/bluez/" + scanProcess.adapterDeviceName + "/dev_";

		scanPropertiesChangedThread = new ScanPropertiesChangedThread(this.scanProcess, queue);
		scanPropertiesChangedThread.setDaemon(true);
		scanPropertiesChangedThread.start();
	}

	@Override
	public void handle(PropertiesChanged properties) {
		LOG.trace(scanProcess.logPrefix + "path:{} sig:{} interface:{}", properties.getPath(), properties.getName(), properties.getInterfaceName());
		if (!properties.getPath().startsWith(prefix) || !properties.getInterfaceName().equals("org.bluez.Device1")) {
			return;
		}

		String address = properties.getPath().replace(prefix, "").replaceAll("_", ":").trim();
		if (!address.matches("^[0-9a-zA-Z:]+$")) {
			return;
		}

		queue.offer(new ScanPropertiesChangedData(address, properties, new Date()));
	}

	class ScanPropertiesChangedData {
		protected final String address;
		protected final PropertiesChanged properties;
		protected final Date date;

		public ScanPropertiesChangedData(String address, PropertiesChanged properties, Date date) {
			this.address = address;
			this.properties = properties;
			this.date = date;
		}
	}

	class ScanPropertiesChangedThread extends Thread {
		private final Logger LOG = LoggerFactory.getLogger(ScanPropertiesChangedThread.class);

		private final ScanProcess scanProcess;
		private final BlockingQueue<ScanPropertiesChangedData> queue;

		private final Map<String, String> nameMap = new HashMap<String, String>();

		public ScanPropertiesChangedThread(ScanProcess scanProcess, BlockingQueue<ScanPropertiesChangedData> queue) {
			this.scanProcess = scanProcess;
			this.queue = queue;
		}

		public void run() {
			while (true) {
				try {
					ScanPropertiesChangedData data = queue.take();

					if (!scanProcess.deviceMap.containsKey(data.address)) {
						Device1 device = DbusHelper.getRemoteObject(scanProcess.manager.getDbusConnection(), data.properties.getPath(), Device1.class);
						scanProcess.deviceMap.put(data.address, new BluetoothDevice(device, scanProcess.adapter, data.properties.getPath(), scanProcess.manager.getDbusConnection()));
						LOG.debug(scanProcess.logPrefix + "{} added to deviceMap.", data.address);
					}

					int rssi = ScanData.RSSI_UNSET;
					int txPower = ScanData.TXPOWER_UNSET;

					Map<String, Variant<?>> map = data.properties.getPropertiesChanged();
					Iterator<Map.Entry<String, Variant<?>>> it = map.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry<String, Variant<?>> entry = it.next();
						String key = entry.getKey();
						Variant<?> value = entry.getValue();

						LOG.trace(scanProcess.logPrefix + "address:{} key:{} class:{} sig:{} type:{} value:{}",
								data.address, key, value.getClass().getName(), value.getSig(), value.getType(), value.getValue());

						if (key.equals("RSSI")) {
							rssi = ((Short)(value.getValue())).intValue();
						} else if (key.equals("TxPower")) {
							txPower = ((Short)(value.getValue())).intValue();
						}
					}

					if (!nameMap.containsKey(data.address)) {
						nameMap.put(data.address, scanProcess.deviceMap.get(data.address).getName());
						LOG.debug(scanProcess.logPrefix + "{} ({}) added to nameMap.", data.address, nameMap.get(data.address));
					}

					ScanData scanData = new ScanData(scanProcess.adapterDeviceName, data.address, nameMap.get(data.address), rssi, txPower, data.date);
					LOG.trace(scanData.toString());
					scanProcess.scanHandler.handle(scanProcess.deviceMap.get(data.address), scanData);
				} catch (InterruptedException e) {
					LOG.warn("caught - {}", e.toString());
					break;
				}
			}
		}
	}
}

/******************************************************************************************************************
 * Sample implementation of IScanHandler interface
 ******************************************************************************************************************/
class MyScanHandler implements IScanHandler {
	private static final Logger LOG = LoggerFactory.getLogger(MyScanHandler.class);

	@Override
	public void handle(BluetoothDevice device, ScanData data) {
		LOG.info(device.toString());
		LOG.info(data.toString());
	}
}
