import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "password123"; // choose your test password
        String hashedPassword = encoder.encode(rawPassword);
        System.out.println("BCrypt hash: " + hashedPassword);
    }
}
