import com.thingmagic.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import com.thingmagic.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RaceTimingApplication {
    private static final Logger logger = LoggerFactory.getLogger(RaceTimingApplication.class);

    public static void main(String[] args) {

        try {
            long epochMillis = System.currentTimeMillis();
            String csvFilePath = "/home/ckkrupina/Plocha/rfid_readings_" + epochMillis + ".csv";
            String readerURI = "tmr:///dev/ttyUSB0";
            Map<String, Integer> readCounts = new HashMap<>();
            Reader rfidReader = Reader.create(readerURI);
            rfidReader.connect();

            FileWriter fw = new FileWriter(csvFilePath, true);
            PrintWriter writer = new PrintWriter(fw, true);
            File csvFile = new File(csvFilePath);
            if (csvFile.length() == 0) {
                writer.println("EPC,RSS,TIME");
            }

            // Nastavenie parametrov čítačky
            ReadPlan plan = new SimpleReadPlan(null, TagProtocol.GEN2, null, null, 100);
            rfidReader.paramSet("/reader/read/plan", plan);
            rfidReader.paramSet("/reader/region/id", Reader.Region.EU4);
            rfidReader.paramSet("/reader/radio/readPower", 2500);
            rfidReader.paramSet("/reader/gen2/session", Gen2.Session.S1);

            // Listener pre čítanie tagov
            rfidReader.addReadListener((r, tr) -> {
                String epc = tr.epcString();
                Number rssi = tr.getRssi();
                long timestamp = tr.getTime();

                // Get read counts
                int count = readCounts.getOrDefault(epc, 0);

                if (count < 2) {
                    readCounts.put(epc, count + 1);

                    // Console
                    logger.info("Tag read: EPC={}, RSSI: {}, Time: {}", epc, rssi, timestamp);
                    // CSV
                    writer.printf("%s,%s,%d%n", epc, rssi, timestamp);
                } else {
                    // Console
                    logger.info("Tag read ignored: EPC={}, RSSI: {}, Time: {}", epc, rssi, timestamp);
                }
            });

            // Spustenie kontinuálneho čítania
            rfidReader.startReading();

            logger.info("Continuous reading started. Press Enter to stop...");

            // Čakanie na vstup používateľa pre zastavenie
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String line;

            while ((line = br.readLine()) != null) {
                if (line.trim().equalsIgnoreCase("q")) {
                    break;
                }
            }

            // Zastavenie čítania
            rfidReader.stopReading();
            rfidReader.destroy();

            logger.info("Reading stopped.");
        } catch (ReaderException e) {
            logger.error("RFID reader initialization error: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
        }
    }
}
