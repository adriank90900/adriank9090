import java.util.concurrent.TimeUnit;

public class Demo {
    public static void main(String[] args) throws Exception {
        try (TwoMaps<String, String> maps = new TwoMaps<>(200, TimeUnit.MILLISECONDS)) {

            maps.put("k", "A", 2, TimeUnit.SECONDS);
            System.out.println("t=0.0s get(k) = " + maps.get("k"));

            Thread.sleep(1000);
            maps.put("k", "B", 2, TimeUnit.SECONDS);

            Thread.sleep(1500);
            System.out.println("t=2.5s get(k) = " + maps.get("k"));

            Thread.sleep(700);
            System.out.println("t=3.2s get(k) = " + maps.get("k"));
        }
    }
}