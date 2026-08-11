import java.util.Scanner;

public class LoginSystem {
    // Simulated stored credentials
    private static final String STORED_USERNAME = "admin";
    private static final String STORED_PASSWORD = "password123";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Welcome! Please log in.");
        
        // Get user input
        System.out.print("Username: ");
        String username = scanner.nextLine();
        
        System.out.print("Password: ");
        String password = scanner.nextLine();
        
        // Validate credentials
        if (authenticate(username, password)) {
            System.out.println("Login successful! Welcome, " + username + ".");
        } else {
            System.out.println("Login failed! Invalid credentials.");
        }
        
        scanner.close();
    }

    public static boolean authenticate(String username, String password) {
        // Compare input with stored credentials
        return username.equals(STORED_USERNAME) && password.equals(STORED_PASSWORD);
    }
}
