import com.thingmagic.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

public class RaceTimingApplication {

    private static String clientIp;
    private static HttpClient httpClient;

    public static void main(String[] args) {
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
                String epc = tr.epcString();
                Number rssi = tr.getRssi();
                long timestamp = tr.getTime();

                System.out.println("Tag read: EPC=" + epc + ", RSSI: " + rssi + ", Time: " + timestamp);
            });

            // Spustenie kontinuálneho čítania
            rfidReader.startReading();
            
            System.out.println("Continuous reading started. Press Enter to stop...");
            
            // Čakanie na vstup používateľa pre zastavenie
            System.in.read();
            
            // Zastavenie čítania
            rfidReader.stopReading();
            rfidReader.destroy();
            
            System.out.println("Reading stopped.");

        } catch (ReaderException e) {
            System.err.println("RFID reader initialization error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }
}