package tests;
import org.mindrot.jbcrypt.BCrypt;
public class GerarHash {
    public static void main(String[] args) {
        String hash = BCrypt.hashpw("admin", BCrypt.gensalt());
        System.out.println(hash);
    }
}
