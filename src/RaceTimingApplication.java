import com.thingmagic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.concurrent.*;

public class RaceTimingApplication {
    private static final Logger logger = LoggerFactory.getLogger(RaceTimingApplication.class);

    public static void main(String[] args) {
        try {
            long epochMillis = System.currentTimeMillis();
            String csvFilePath = "/home/ckkrupina/Plocha/rfid_readings_" + epochMillis + ".csv";
            String readerURI = "tmr:///dev/ttyUSB0";

            // Fronta pre tagy
            ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
            // Map pre kontrolu opakovaných čítaní
            Map<String, Long> lastRead = new ConcurrentHashMap<>();

            Reader rfidReader = Reader.create(readerURI);
            rfidReader.connect();

            // Nastavenie parametrov čítačky
            rfidReader.paramSet("/reader/region/id", Reader.Region.EU4);
            rfidReader.paramSet("/reader/radio/readPower", 2500);
            rfidReader.paramSet("/reader/gen2/session", Gen2.Session.S1);
            int[] antennas = {1}; // uprav podľa počtu antén
            ReadPlan plan = new SimpleReadPlan(antennas, TagProtocol.GEN2, null, null, 100);
            rfidReader.paramSet("/reader/read/plan", plan);

            // CSV hlavička
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(csvFilePath, true)))) {
                File csvFile = new File(csvFilePath);
                if (csvFile.length() == 0) {
                    writer.println("EPC,RSSI,TIME");
                }
            }

            // Listener pre čítanie tagov
            rfidReader.addReadListener((r, tr) -> {
                String epc = tr.epcString();
                Number rssi = tr.getRssi();
                long timestamp = tr.getTime();

                // Cooldown 5 sekúnd pre opakované čítanie
                long now = System.currentTimeMillis();
                if (!lastRead.containsKey(epc) || now - lastRead.get(epc) > 5000) {
                    lastRead.put(epc, now);
                    queue.add(epc + "," + rssi + "," + timestamp);
                }
            });

            // Samostatný thread na zapisovanie CSV a logovanie
            Thread writerThread = new Thread(() -> {
                try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(csvFilePath, true)))) {
                    while (true) {
                        String line = queue.poll();
                        if (line != null) {
                            writer.println(line);
                            logger.info(line);
                        } else {
                            Thread.sleep(10);
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    logger.error("Writer thread error: {}", e.getMessage());
                }
            });
            writerThread.setDaemon(true); // aby sa ukončil pri skončení main threadu
            writerThread.start();

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
