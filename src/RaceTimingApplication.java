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
        // Kontrola argumentov
        if (args.length == 0) {
            System.err.println("Usage: java RaceTimingApplication <client_ip>");
            System.err.println("Example: java RaceTimingApplication 192.168.1.100");
            return;
        }
        
        clientIp = args[0];
        System.out.println("Starting application with client IP: " + clientIp);
        
        // Inicializácia HTTP klienta
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

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
                
                // Odoslanie HTTP requestu
                sendHttpRequest(epc, rssi, timestamp);
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
    
    private static void sendHttpRequest(String epc, Number rssi, long timestamp) {
        try {
            // Vytvorenie JSON payload
            String jsonPayload = String.format(
                "{\"epc\":\"%s\",\"rssi\":%s,\"timestamp\":%d}",
                epc, rssi, timestamp
            );
            
            // Vytvorenie HTTP requestu
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + clientIp + ":3000/api/v1/tag_reads")) // Upravte port podľa potreby
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            
            // Asynchrónne odoslanie requestu
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        System.out.println("HTTP Response: " + response.statusCode() + 
                                         " - " + response.body());
                    })
                    .exceptionally(throwable -> {
                        System.err.println("HTTP Request failed: " + throwable.getMessage());
                        return null;
                    });
                    
        } catch (Exception e) {
            System.err.println("Error sending HTTP request: " + e.getMessage());
        }
    }
}