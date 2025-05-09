import java.sql.*;
import java.util.Scanner;

public class Library {
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n--- Library Management System ---");
            System.out.println("1. Add New Book");
            System.out.println("2. View Available Books");
            System.out.println("3. Issue Book");
            System.out.println("4. Return Book");
            System.out.println("5. Exit");
            System.out.print("Select an option: ");
            int choice = Integer.parseInt(scanner.nextLine());

            switch (choice) {
                case 1:
                    addNewBook();
                    break;
                case 2:
                    viewAvailableBooks();
                    break;
                case 3:
                    issueBook();
                    break;
                case 4:
                    returnBook();
                    break;
                case 5:
                    System.out.println("Exiting system. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void addNewBook() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.print("Enter Book Title: ");
            String title = scanner.nextLine();
            System.out.print("Enter Author Name: ");
            String author = scanner.nextLine();
            System.out.print("Enter Quantity: ");
            int quantity = Integer.parseInt(scanner.nextLine());

            String sql = "INSERT INTO books (title, author, quantity) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, title);
            pstmt.setString(2, author);
            pstmt.setInt(3, quantity);
            pstmt.executeUpdate();
            System.out.println("Book added successfully.");
        } catch (SQLException e) {
            System.out.println("Error adding book: " + e.getMessage());
        }
    }

    private static void viewAvailableBooks() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM books WHERE quantity > 0";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\nAvailable Books:");
            while (rs.next()) {
                System.out.printf("ID: %d, Title: %s, Author: %s, Quantity: %d%n",
                        rs.getInt("id"), rs.getString("title"),
                        rs.getString("author"), rs.getInt("quantity"));
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving books: " + e.getMessage());
        }
    }

    private static void issueBook() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.print("Enter Book ID to Issue: ");
            int bookId = Integer.parseInt(scanner.nextLine());
            System.out.print("Enter User Name: ");
            String userName = scanner.nextLine();
            System.out.print("Enter Issue Date (YYYY-MM-DD): ");
            String issueDate = scanner.nextLine();

            // Check if book is available
            String checkSql = "SELECT quantity FROM books WHERE id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, bookId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                int quantity = rs.getInt("quantity");
                if (quantity > 0) {
                    // Issue the book
                    String issueSql = "INSERT INTO issued_books (book_id, user_name, issue_date) VALUES (?, ?, ?)";
                    PreparedStatement issueStmt = conn.prepareStatement(issueSql);
                    issueStmt.setInt(1, bookId);
                    issueStmt.setString(2, userName);
                    issueStmt.setDate(3, Date.valueOf(issueDate));
                    issueStmt.executeUpdate();

                    // Update book quantity
                    String updateSql = "UPDATE books SET quantity = quantity - 1 WHERE id = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                    updateStmt.setInt(1, bookId);
                    updateStmt.executeUpdate();

                    System.out.println("Book issued successfully.");
                } else {
                    System.out.println("Book is not available.");
                }
            } else {
                System.out.println("Book ID not found.");
            }
        } catch (SQLException e) {
            System.out.println("Error issuing book: " + e.getMessage());
        }
    }

    private static void returnBook() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.print("Enter Issued Book ID: ");
            int issuedId = Integer.parseInt(scanner.nextLine());
            System.out.print("Enter Return Date (YYYY-MM-DD): ");
            String returnDateStr = scanner.nextLine();

            // Retrieve issue details
            String selectSql = "SELECT book_id, issue_date FROM issued_books WHERE id = ?";
            PreparedStatement selectStmt = conn.prepareStatement(selectSql);
            selectStmt.setInt(1, issuedId);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                int bookId = rs.getInt("book_id");
                Date issueDate = rs.getDate("issue_date");
                Date returnDate = Date.valueOf(returnDateStr);

                // Calculate fine (assuming 14-day borrowing period and $1 per day fine)
                long diff = returnDate.getTime() - issueDate.getTime();
                long days = diff / (1000 * 60 * 60 * 24);
                double fine = 0;
                if (days > 14) {
                    fine = (days - 14) * 1.0;
                }

                // Update issued_books record
                String updateIssuedSql = "UPDATE issued_books SET return_date = ?, fine = ? WHERE id = ?";
                PreparedStatement updateIssuedStmt = conn.prepareStatement(updateIssuedSql);
                updateIssuedStmt.setDate(1, returnDate);
                updateIssuedStmt.setDouble(2, fine);
                updateIssuedStmt.setInt(3, issuedId);
                updateIssuedStmt.executeUpdate();

                // Update book quantity
                String updateBookSql = "UPDATE books SET quantity = quantity + 1 WHERE id = ?";
                PreparedStatement updateBookStmt = conn.prepareStatement(updateBookSql);
                updateBookStmt.setInt(1, bookId);
                updateBookStmt.executeUpdate();

                System.out.println("Book returned successfully.");
                if (fine > 0) {
                    System.out.printf("Late return fine: $%.2f%n", fine);
                }
            } else {
                System.out.println("Issued Book ID not found.");
            }
        } catch (SQLException e) {
            System.out.println("Error returning book: " + e.getMessage());
        }
    }
}
.