package com.jiashie.oaidfacade;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

/**
 * 模拟器识别的工具类
 *
 * @see "https://github.com/strazzere/anti-emulator"
 */
public class FindEmulator {
    private static final String TAG = "FindEmulator";

    // Need to check the format of these
    // Android emulator support up to 16 concurrent emulator
    // The console of the first emulator instance running on a given
    // machine uses console port 5554
    // Subsequent instances use port numbers increasing by two
    private static final String[] known_numbers = {
            "15555215554", // Default emulator phone numbers + VirusTotal
            "15555215556", "15555215558", "15555215560", "15555215562", "15555215564", "15555215566",
            "15555215568", "15555215570", "15555215572", "15555215574", "15555215576", "15555215578",
            "15555215580", "15555215582", "15555215584",};
    private static final String[] known_device_ids = {"000000000000000", // Default emulator id
            "e21833235b6eef10", // VirusTotal id
            "012345678912345"};
    private static final String[] known_imsi_ids = {"310260000000000" // Default imsi id
    };
    private static final String[] known_pipes = {"/dev/socket/qemud", "/dev/qemu_pipe"};
    private static final String[] known_files = {"/system/lib/libc_malloc_debug_qemu.so", "/sys/qemu_trace"/*,
            "/system/bin/qemu-props"*/};
    private static final String[] known_geny_files = {"/dev/socket/genyd", "/dev/socket/baseband_genyd"};
    private static final String[] known_qemu_drivers = {"goldfish"};
    //https://github.com/strazzere/anti-emulator/issues/7
    private static final String[] known_other_files = {
            "/data/youwave_id",
            "/dev/vboxguest",
            "/dev/vboxuser",
            "/fstab.vbox86",
            "/init.vbox86.rc",
            "/system/lib/vboxguest.ko",
            "/system/lib/vboxsf.ko",
            "/system/lib/vboxvideo.ko",
            "/system/usr/idc/androVM_Virtual_Input.idc",
            "/system/usr/keylayout/androVM_Virtual_Input.kl",
            "/system/xbin/mount.vboxsf",
            "/ueventd.vbox86.rc",
            "/system/lib/hw/audio.primary.vbox86.so",
            "/system/lib/hw/camera.vbox86.so",
            "/system/lib/hw/gps.vbox86.so",
            "/system/lib/hw/gralloc.vbox86.so",
            "/system/lib/hw/sensors.vbox86.so",
            "/data/app/com.bluestacks.appmart-1.apk",
            "/data/app/com.bluestacks.BstCommandProcessor-1.apk",
            "/data/app/com.bluestacks.help-1.apk",
            "/data/app/com.bluestacks.home-1.apk",
            "/data/app/com.bluestacks.s2p-1.apk",
            "/data/app/com.bluestacks.searchapp-1.apk",
            "/data/bluestacks.prop",
            "/data/data/com.androVM.vmconfig",
            "/data/data/com.bluestacks.accelerometerui",
            "/data/data/com.bluestacks.appfinder",
            "/data/data/com.bluestacks.appmart",
            "/data/data/com.bluestacks.appsettings",
            "/data/data/com.bluestacks.BstCommandProcessor",
            "/data/data/com.bluestacks.bstfolder",
            "/data/data/com.bluestacks.help",
            "/data/data/com.bluestacks.home",
            "/data/data/com.bluestacks.s2p",
            "/data/data/com.bluestacks.searchapp",
            "/data/data/com.bluestacks.settings",
            "/data/data/com.bluestacks.setup",
            "/data/data/com.bluestacks.spotlight",
            "/mnt/prebundledapps/bluestacks.prop.orig",
            "/mnt/prebundledapps/propfiles/ics.bluestacks.prop.note",
            "/mnt/prebundledapps/propfiles/ics.bluestacks.prop.s2",
            "/mnt/prebundledapps/propfiles/ics.bluestacks.prop.s3",
            "/mnt/sdcard/bstfolder/InputMapper/com.bluestacks.appmart.cfg",
            "/mnt/sdcard/buildroid-gapps-ics-20120317-signed.tgz",
            "/mnt/sdcard/windows/InputMapper/com.bluestacks.appmart.cfg",
            "/proc/irq/9/vboxguest",
            "/sys/bus/pci/drivers/vboxguest",
            "/sys/bus/pci/drivers/vboxguest/0000:00:04.0",
            "/sys/bus/pci/drivers/vboxguest/bind",
            "/sys/bus/pci/drivers/vboxguest/module",
            "/sys/bus/pci/drivers/vboxguest/new_id",
            "/sys/bus/pci/drivers/vboxguest/remove_id",
            "/sys/bus/pci/drivers/vboxguest/uevent",
            "/sys/bus/pci/drivers/vboxguest/unbind",
            "/sys/bus/platform/drivers/qemu_pipe",
            "/sys/bus/platform/drivers/qemu_trace",
            "/sys/class/bdi/vboxsf-c",
            "/sys/class/misc/vboxguest",
            "/sys/class/misc/vboxuser",
            "/sys/devices/virtual/bdi/vboxsf-c",
            "/sys/devices/virtual/misc/vboxguest",
            "/sys/devices/virtual/misc/vboxguest/dev",
            "/sys/devices/virtual/misc/vboxguest/power",
            "/sys/devices/virtual/misc/vboxguest/subsystem",
            "/sys/devices/virtual/misc/vboxguest/uevent",
            "/sys/devices/virtual/misc/vboxuser",
            "/sys/devices/virtual/misc/vboxuser/dev",
            "/sys/devices/virtual/misc/vboxuser/power",
            "/sys/devices/virtual/misc/vboxuser/subsystem",
            "/sys/devices/virtual/misc/vboxuser/uevent",
            "/sys/module/vboxguest/",
            "/sys/module/vboxsf/",
            "/sys/module/vboxvideo/",
            "/system/app/bluestacksHome.apk",
            "/system/bin/androVM-prop",
            "/system/bin/androVM-vbox-sf",
            "/system/bin/androVM_setprop",
            "/system/bin/get_androVM_host",
            "/system/bin/mount.vboxsf",
            "/system/etc/init.androVM.sh",
            "/system/etc/init.buildroid.sh",
            "/system/lib/modules/3.0.8-android-x86+/extra/vboxguest",
            "/system/lib/modules/3.0.8-android-x86+/extra/vboxguest/vboxguest.ko",
            "/system/lib/modules/3.0.8-android-x86+/extra/vboxsf",
            "/system/lib/modules/3.0.8-android-x86+/extra/vboxsf/vboxsf.ko",
            "/ueventd.android_x86.rc",
            //逍遥模拟器
            "/sys/devices/virtual/misc/microcode",
            "/data/data/com.microvirt.launcher",
            "/data/data/com.microvirt.installer",
            "/data/data/com.microvirt.market",
            "/data/data/com.microvirt.memuime",
            "/data/data/com.microvirt.tools",
            "/data/data/com.microvirt.guide",
            //网易mumu
            "/system/bin/nemuVM-nemu-sf",
            "/system/bin/nemuVM-nemu-service",
            "/system/bin/mount.nemusf",
            "/system/bin/nemu-prop",
            "/lib/nemusf.ko",
            "/lib/nemuvideo.ko"
    };
    /**
     * Known props, in the format of [property name, value to seek] if value to seek is null, then it is assumed that
     * the existence of this property (anything not null) indicates the QEmu environment.
     */
    private static final Property[] known_props = {new Property("init.svc.qemud", null),
            new Property("init.svc.qemu-props", null), new Property("qemu.hw.mainkeys", null),
            new Property("qemu.sf.fake_camera", null), new Property("qemu.sf.lcd_density", null),
            new Property("ro.bootloader", "unknown"), new Property("ro.bootmode", "unknown"),
            new Property("ro.hardware", "goldfish"), new Property("ro.kernel.android.qemud", null),
            new Property("ro.kernel.qemu.gles", null), new Property("ro.kernel.qemu", "1"),
            new Property("ro.product.device", "generic"), new Property("ro.product.model", "sdk"),
            new Property("ro.product.name", "sdk"),
            // Need to double check that an "empty" string ("") returns null
            new Property("ro.serialno", null)};
    /**
     * The "known" props have the potential for false-positiving due to interesting (see: poorly) made Chinese
     * devices/odd ROMs. Keeping this threshold low will result in better QEmu detection with possible side affects.
     */
    private static final int MIN_PROPERTIES_THRESHOLD = 0x5;

    /**
     * Check the existence of known pipes used by the Android QEmu environment.
     *
     * @return {@code true} if any pipes where found to exist or {@code false} if not.
     */
    public static boolean hasPipes() {
        for (String pipe : known_pipes) {
            File qemu_socket = new File(pipe);
            if (qemu_socket.exists()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check the existence of known files used by the Android QEmu environment.
     *
     * @return {@code true} if any files where found to exist or {@code false} if not.
     */
    public static boolean hasQEmuFiles() {
        for (String pipe : known_files) {
            File qemu_file = new File(pipe);
            if (qemu_file.exists()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check the existence of known files used by the Genymotion environment.
     *
     * @return {@code true} if any files where found to exist or {@code false} if not.
     */
    public static boolean hasGenyFiles() {
        for (String file : known_geny_files) {
            File geny_file = new File(file);
            if (geny_file.exists()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Reads in the driver file, then checks a list for known QEmu drivers.
     *
     * @return {@code true} if any known drivers where found to exist or {@code false} if not.
     */
    public static boolean hasQEmuDrivers() {
        for (File drivers_file : new File[]{new File("/proc/tty/drivers"), new File("/proc/cpuinfo")}) {
            if (drivers_file.exists() && drivers_file.canRead()) {
                // We don't care to read much past things since info we care about should be inside here
                byte[] data = new byte[1024];
                try {
                    InputStream is = new FileInputStream(drivers_file);
                    is.read(data);
                    is.close();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }

                String driver_data = new String(data);
                for (String known_qemu_driver : known_qemu_drivers) {
                    if (driver_data.contains(known_qemu_driver)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean hasKnownPhoneNumber(Context context) {
        try {
            if (hasPermission(context)) {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

                @SuppressLint("MissingPermission")
                String phoneNumber = telephonyManager.getLine1Number();

                for (String number : known_numbers) {
                    if (number.equalsIgnoreCase(phoneNumber)) {
                        return true;
                    }

                }
            }
        } catch (Throwable t) {
            //most probably, no permission
        }
        return false;
    }

    public static boolean hasKnownDeviceId(Context context) {

        try {
            if (hasPermission(context)) {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                @SuppressLint("MissingPermission")
                String deviceId = telephonyManager.getDeviceId();

                for (String known_deviceId : known_device_ids) {
                    if (known_deviceId.equalsIgnoreCase(deviceId)) {
                        return true;
                    }

                }
            }
        } catch (Throwable t) {
            //most probably, no permission
        }
        return false;

    }

    public static boolean hasKnownImsi(Context context) {
        try {
            if (hasPermission(context)) {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                @SuppressLint("MissingPermission")
                String imsi = telephonyManager.getSubscriberId();

                for (String known_imsi : known_imsi_ids) {
                    if (known_imsi.equalsIgnoreCase(imsi)) {
                        return true;
                    }
                }
            }
        } catch (Throwable t) {
            //most probably, no permission
        }

        return false;
    }

    private static boolean hasPermission(Context context) {
        return context.getPackageManager().checkPermission(Manifest.permission.READ_PHONE_STATE, context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasEmulatorBuild(Context context) {
        //String BOARD = android.os.Build.BOARD; // The name of the underlying board, like "unknown".
        // This appears to occur often on real hardware... that's sad
        // String BOOTLOADER = android.os.Build.BOOTLOADER; // The system bootloader version number.
        String BRAND = android.os.Build.BRAND; // The brand (e.g., carrier) the software is customized for, if any.
        // "generic"
        String DEVICE = android.os.Build.DEVICE; // The name of the industrial design. "generic"
        String HARDWARE = android.os.Build.HARDWARE; // The name of the hardware (from the kernel command line or
        // /proc). "goldfish"
        String MODEL = android.os.Build.MODEL; // The end-user-visible name for the end product. "sdk"
        String PRODUCT = android.os.Build.PRODUCT; // The name of the overall product.
        /*(BOARD.compareTo("unknown") == 0)*/
        /* || (BOOTLOADER.compareTo("unknown") == 0) */
        return (BRAND.compareTo("generic") == 0) || (DEVICE.compareTo("generic") == 0)
                || (MODEL.compareTo("sdk") == 0) || (PRODUCT.compareTo("sdk") == 0)
                || (HARDWARE.compareTo("goldfish") == 0);
    }

    public static boolean isOperatorNameAndroid(Context paramContext) {
        String szOperatorName = ((TelephonyManager) paramContext.getSystemService(Context.TELEPHONY_SERVICE)).getNetworkOperatorName();
        boolean isAndroid = szOperatorName.equalsIgnoreCase("android");
        return isAndroid;
    }

    /**
     * Will query specific system properties to try and fingerprint a QEmu environment. A minimum threshold must be met
     * in order to prevent false positives.
     *
     * @param context A {link Context} object for the Android application.
     * @return {@code true} if enough properties where found to exist or {@code false} if not.
     */
    public boolean hasQEmuProps(Context context) {
        int found_props = 0;

        for (Property property : known_props) {
            String property_value = getProp(context, property.name);
            // See if we expected just a non-null
            if ((property.seek_value == null) && (property_value != null)) {
                found_props++;
            }
            // See if we expected a value to seek
            if ((property.seek_value != null) && (property_value != null && property_value.contains(property.seek_value))) {
                found_props++;
            }

        }

        return found_props >= MIN_PROPERTIES_THRESHOLD;
    }

    public static boolean hasOtherKnownFiles() {
        for (String file : known_other_files) {
            File f = new File(file);
            if (f.exists()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Simple class used for the systems properties
     * checked in later places of the application.
     *
     * @author tstrazzere
     */
    public static class Property {
        public String name;
        public String seek_value;

        public Property(String name, String seek_value) {
            this.name = name;
            this.seek_value = seek_value;
        }
    }

    /**
     * Method to reflectively invoke the SystemProperties.get command - which is the equivalent to the adb shell getProp
     * command.
     *
     * @param context  A {@link Context} object used to get the proper ClassLoader (just needs to be Application Context
     *                 object)
     * @param property A {@code String} object for the property to retrieve.
     * @return {@code String} value of the property requested.
     */
    private static String getProp(Context context, String property) {
        try {
            ClassLoader classLoader = context.getClassLoader();
            Class<?> systemProperties = classLoader.loadClass("android.os.SystemProperties");

            Method get = systemProperties.getMethod("get", String.class);

            Object[] params = new Object[1];
            params[0] = property;

            return (String) get.invoke(systemProperties, params);
        } catch (IllegalArgumentException iAE) {
            return null;
        } catch (Exception exception) {
            return null;
        }
    }

    private static int emulator;

    public static boolean isEmulator(Context context) {
        if (emulator == 0) {
            try {
                boolean isemu = hasKnownDeviceId(context)
                        || hasKnownImsi(context)
                        || hasEmulatorBuild(context)
                        || hasKnownPhoneNumber(context) || hasPipes()
                        || hasQEmuDrivers()
                        || hasQEmuFiles()
                        || hasGenyFiles()
                        || hasOtherKnownFiles();
                emulator = isemu ? 1 : -1;
            } catch (Throwable t) {
                //
            }
        }
        return emulator == 1;
    }
}
