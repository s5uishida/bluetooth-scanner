# bluetooth-scanner
bluetooth-scanner is a bluetooth scanner java library using [bluez-dbus](https://github.com/hypfvieh/bluez-dbus) with [BlueZ](http://www.bluez.org/) version 5.50 on linux OS. I releases this in the form of the Eclipse plug-in project.
You need Java 8 or higher.

It uses the dbus PropertiesChanged mechanism to catch the advertising signal and provide the BluetoothDevice object and RSSI / TxPower data to the handler.

The purpose of this library is to use an advertising signal to trigger the connection to a bluetooth device.

I have confirmed that it works in Raspberry Pi 3B ([Raspbian Buster Lite OS](https://www.raspberrypi.org/downloads/raspbian/) (2019-07-10)).

## Install Raspbian Buster Lite OS (2019-07-10)
The reason for using this version is that it is the latest as of July 2019 and [BlueZ](http://www.bluez.org/) 5.50 is included from the beginning.

## Install jdk11 on Raspberry Pi 3B
For example, [jdk11 apt-install](https://apt.bell-sw.com/) at [BELLSOFT](https://bell-sw.com/) is shown below.
```
# wget -q -O - https://download.bell-sw.com/pki/GPG-KEY-bellsoft | apt-key add -
# echo "deb [arch=armhf] https://apt.bell-sw.com/ stable main" | tee /etc/apt/sources.list.d/bellsoft.list
# apt-get update
# apt-get install bellsoft-java11
```

## Install git
If git is not included, please install it.
```
# apt-get install git
```

## Use this with the following bundles
- [SLF4J 1.7.26](https://www.slf4j.org/)
- [bluez-dbus-osgi 0.1.2-SNAPSHOT](https://github.com/hypfvieh/bluez-dbus)

I would like to thank the authors of these very useful codes, and all the contributors.

## How to use
The following sample code implementing the `IScanHandler` interface will be helpful.
```java
import com.github.hypfvieh.bluetooth.DiscoveryFilter;
import com.github.hypfvieh.bluetooth.DiscoveryTransport;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;

import io.github.s5uishida.iot.bluetooth.scanner.IScanHandler;
import io.github.s5uishida.iot.bluetooth.scanner.ScanData;
import io.github.s5uishida.iot.bluetooth.scanner.ScanProcess;

public class MyScan {
    public static void main(String[] args) throws IOException, InterruptedException {
        Map<DiscoveryFilter, Object> filter = new HashMap<DiscoveryFilter, Object>();
        filter.put(DiscoveryFilter.Transport, DiscoveryTransport.LE);
        
        ScanProcess scanProcess = new ScanProcess("hci0", new MyScanHandler(), filter);
        scanProcess.start();
    }
}

class MyScanHandler implements IScanHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MyScanHandler.class);
    
    @Override
    public void handle(BluetoothDevice device, ScanData data) {
        LOG.info(device.toString());
        LOG.info(data.toString());
    }
}
```
