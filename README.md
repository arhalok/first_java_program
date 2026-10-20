# Solar Equipment Maintenance Tracker (Java Swing + MySQL)

Student MVP based on the project SRS. Intended for JDK 21, VS Code, and MySQL. It provides a login screen, a dashboard, and basic add/list/delete screens for equipment, maintenance schedules, maintenance records, equipment parts, and (Administrator only) users. It creates the required tables on startup.

## First: verify MySQL login
You already reached the MySQL prompt and selected `solar_maintenance`. In MySQL, run `SHOW TABLES;`. If it returns `Empty set`, this app will create the tables when launched.

## Running in GitHub Codespaces / Linux

Because Codespaces is a headless Linux environment, a virtual display and web viewer (noVNC) are configured to let you see and interact with the Java Swing GUI directly in your browser:

1. Run the launcher script:
   ```bash
   ./start_gui.sh
   ```
2. In VS Code, go to the **Ports** tab (bottom panel next to Terminal).
3. Find port **6080** and click the globe icon (**Open in Browser**) or open the forwarded address.
4. You will see the **Solar Equipment Maintenance Tracker** graphical interface running live.

## Configure database credentials
The app defaults to:
- URL: `jdbc:mysql://localhost:3306/solar_maintenance?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true`
- User: `root`
- Password: empty

Because your MySQL root account has a password on Windows, set the `SOLAR_DB_PASSWORD` environment variable before starting the app. In Windows CMD:

```bat
set SOLAR_DB_USER=root
set SOLAR_DB_PASSWORD=YOUR_MYSQL_ROOT_PASSWORD
mvn clean package
mvn exec:java -Dexec.mainClass=com.piet.solar.App
```

Replace `YOUR_MYSQL_ROOT_PASSWORD` locally; do not share it in chat. Alternatively, edit `App.java` and change `DB_PASSWORD` default, but avoid committing real passwords to GitHub.

## Run in VS Code
1. Install JDK 21 and Maven, plus the VS Code Extension Pack for Java.
2. Open the whole `Solar_Equipment_Maintenance_Tracker` folder in VS Code.
3. Open a terminal in that folder and run the commands above.
4. If you don't have Maven installed, install it first or use the VS Code Maven extension.

## First login
- Username: `admin`
- Password: `admin123`

The app creates this default admin account if it doesn't already exist. Change this password and do not use this sample account for a real deployment. Passwords are stored as SHA-256 hashes in this educational MVP; for a production system, use a password-specific hash such as Argon2id or bcrypt and stronger account controls.

## Data entry order
1. Equipment: add equipment and note its generated `equipment_id`.
2. Users: administrator can add users; note each generated `user_id`.
3. Maintenance Schedule: enter a valid equipment ID, optional assigned user ID, type, due date, and status.
4. Maintenance Records: enter valid equipment ID and optional schedule/technician IDs.
5. Equipment Parts: enter a valid equipment ID and part information.

Dates must use `YYYY-MM-DD`. For optional numeric foreign keys, leave the field blank. `Viewer` role is shown in the login information but this early MVP does not yet enforce read-only permissions across every tab; role enforcement, editing records, search/filter, CSV/PDF export, and automated overdue status updates remain future improvements.

## Important notes
- MySQL server must be running.
- Keep database credentials private.
- The SRS excludes direct IoT/sensor integration from version 1.
- This is a starter MVP. Run it and test it on your own machine before claiming that features/tests are complete in the college report.
