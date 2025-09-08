import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.highgui.HighGui;

import java.sql.*;
import java.util.Arrays;

public class Main {
    private static final String URL = "jdbc:mysql://localhost:3306/facepay";
    private static final String USER = "root";
    private static final String PASSWORD = "@Arjunyadav0009";

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Load Haar Cascade
        String xmlFile = "/usr/local/share/opencv4/haarcascades/haarcascade_frontalface_default.xml";
        CascadeClassifier faceDetector = new CascadeClassifier(xmlFile);

        if (faceDetector.empty()) {
            System.out.println("❌ Haar Cascade file not loaded!");
            return;
        }

        // Load reference image (registered user)
        Mat reference = Imgcodecs.imread("/Users/arjunyadav/Desktop/reference.jpg", Imgcodecs.IMREAD_GRAYSCALE);
        if (reference.empty()) {
            System.out.println("❌ Reference image not found!");
            return;
        }
        Imgproc.resize(reference, reference, new Size(200, 200));

        // Start webcam
        VideoCapture camera = new VideoCapture(0);
        if (!camera.isOpened()) {
            System.out.println("❌ Camera not found!");
            return;
        }

        Mat frame = new Mat();
        while (true) {
            camera.read(frame);
            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

            MatOfRect faces = new MatOfRect();
            faceDetector.detectMultiScale(gray, faces);

            for (Rect rect : faces.toArray()) {
                Mat face = new Mat(gray, rect);
                Imgproc.resize(face, face, new Size(200, 200));

                // Similarity check
                double similarity = compareHist(reference, face);
                System.out.println("🔍 Similarity: " + similarity);

                if (similarity > 0.8) {
                    String name = "Arjun Kumar";
                    double bill = 1000;

                    createUserIfNotExists(name, 50000);
                    double balance = getBalance(name);
                    System.out.println("💰 Current Balance: " + balance);

                    if (balance >= bill) {
                        deductBalance(name, bill);
                        printInvoice(name, bill, balance - bill);
                        System.out.println("✅ Payment Successful!");
                    } else {
                        System.out.println("❌ Insufficient Balance!");
                    }

                    camera.release();
                    HighGui.destroyAllWindows();
                    System.exit(0);
                }
            }

            HighGui.imshow("Face Recognition Payment", frame);
            if (HighGui.waitKey(30) == 'q') break;
        }

        camera.release();
        HighGui.destroyAllWindows();
    }

    private static double getBalance(String name) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String sql = "SELECT balance FROM users WHERE name=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("balance");
            else System.out.println("⚠️ User not found in DB: " + name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static void deductBalance(String name, double amount) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String sql = "UPDATE users SET balance = balance - ? WHERE name=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setDouble(1, amount);
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createUserIfNotExists(String name, double initialBalance) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String checkSql = "SELECT id FROM users WHERE name=?";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setString(1, name);
            ResultSet rs = checkPs.executeQuery();

            if (!rs.next()) {
                String insertSql = "INSERT INTO users (name, balance) VALUES (?, ?)";
                PreparedStatement insertPs = conn.prepareStatement(insertSql);
                insertPs.setString(1, name);
                insertPs.setDouble(2, initialBalance);
                insertPs.executeUpdate();
                System.out.println("🆕 User created: " + name + " with balance " + initialBalance);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double compareHist(Mat img1, Mat img2) {
        Mat hist1 = new Mat();
        Mat hist2 = new Mat();
        Imgproc.calcHist(Arrays.asList(img1), new MatOfInt(0), new Mat(), hist1, new MatOfInt(256), new MatOfFloat(0, 256));
        Imgproc.calcHist(Arrays.asList(img2), new MatOfInt(0), new Mat(), hist2, new MatOfInt(256), new MatOfFloat(0, 256));
        Core.normalize(hist1, hist1);
        Core.normalize(hist2, hist2);
        return Imgproc.compareHist(hist1, hist2, Imgproc.HISTCMP_CORREL);
    }

    private static void printInvoice(String name, double amount, double balance) {
        System.out.println("\n========================================");
        System.out.println("             FACE PAY RECEIPT          ");
        System.out.println("========================================");
        System.out.printf("Customer Name     : %s%n", name);
        System.out.printf("Amount Paid       : ₹%.2f%n", amount);
        System.out.printf("Available Balance  : ₹%.2f%n", balance);
        System.out.printf("Date             : %s%n", new java.util.Date());
        System.out.println("----------------------------------------");
        System.out.println("   Thank you for using Face Pay!       ");
        System.out.println("========================================\n");
    }
}