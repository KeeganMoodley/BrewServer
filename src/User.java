/**
 * Created by s213463695 on 2016/03/22.
 */
import java.io.Serializable;

/**
 * Created by s213463695 on 2016/03/21.
 */
public class User implements Serializable {

    private String username;
    private String password;
    private String email;

    public User(String email, String username) {
        this.email = email;
        this.username = username;
    }

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }
}
