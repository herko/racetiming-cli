import com.thingmagic.*;

public class RaceTimingApplication {

    public static void main(String[] args) {
        System.out.println("Starting application..");

        try {
            String readerURI = "tmr:///dev/ttyUSB0";

            Reader rfidReader = Reader.create(readerURI);

            rfidReader.connect();

            // Nastavenie parametrov čítačky
            ReadPlan plan = new SimpleReadPlan(null, TagProtocol.GEN2, null, null, 100);
            rfidReader.paramSet("/reader/read/plan", plan);
            rfidReader.paramSet("/reader/region/id", Reader.Region.EU4);
            rfidReader.paramSet("/reader/radio/readPower", 2500);
            rfidReader.paramSet("/reader/gen2/session", Gen2.Session.S1);

            // Pridanie listener-a pre čítanie tagov
            rfidReader.addReadListener((r, tr) -> {
                String tagId = tr.epcString();
                String epc = tr.epcString();
                Number rssi = tr.getRssi();
                long timestamp = tr.getTime();

                System.out.println("Tag read: EPC=" + epc + ", RSSI: " + rssi + ", Time: " + timestamp);
            });

            rfidReader.startReading();

            // Čakanie na vstup používateľa pre zastavenie
            System.in.read();

            // Zastavenie čítania
            rfidReader.stopReading();
            rfidReader.destroy();

            System.out.println("Reading stopped.");

        } catch (ReaderException e) {
            System.err.println("RFID reader initialization error.");
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }
}
