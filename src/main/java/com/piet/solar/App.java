package com.piet.solar;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Solar Equipment Maintenance Tracker - student MVP for JDK 21 + MySQL. */
public class App {
    private static final String URL = System.getenv().getOrDefault("SOLAR_DB_URL", "jdbc:mysql://localhost:3306/solar_maintenance?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true");
    private static final String DB_USER = System.getenv().getOrDefault("SOLAR_DB_USER", "root");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("SOLAR_DB_PASSWORD", "");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                Database.initialize();
                new LoginFrame().setVisible(true);
            } catch (Exception ex) {
                showError("Could not initialize the database. Check MySQL and DB settings.\n\n" + ex.getMessage());
            }
        });
    }

    static Connection connect() throws SQLException { return DriverManager.getConnection(URL, DB_USER, DB_PASSWORD); }
    static void showError(String message) { JOptionPane.showMessageDialog(null, message, "Solar Maintenance Tracker", JOptionPane.ERROR_MESSAGE); }
    static String hash(String value) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(bytes);
    }

    static class Database {
        static void initialize() throws Exception {
            try (Connection c = connect(); Statement s = c.createStatement()) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS users (user_id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, username VARCHAR(60) NOT NULL UNIQUE, password_hash VARCHAR(64) NOT NULL, role ENUM('Administrator','Maintenance Staff','Viewer') NOT NULL DEFAULT 'Viewer', status ENUM('Active','Inactive') NOT NULL DEFAULT 'Active')");
                s.executeUpdate("CREATE TABLE IF NOT EXISTS equipment (equipment_id INT AUTO_INCREMENT PRIMARY KEY, equipment_code VARCHAR(40) NOT NULL UNIQUE, equipment_type VARCHAR(100) NOT NULL, location VARCHAR(150) NOT NULL, installation_date DATE NULL, status ENUM('Active','Under Maintenance','Inactive') NOT NULL DEFAULT 'Active')");
                s.executeUpdate("CREATE TABLE IF NOT EXISTS maintenance_schedule (schedule_id INT AUTO_INCREMENT PRIMARY KEY, equipment_id INT NOT NULL, assigned_to INT NULL, maintenance_type VARCHAR(100) NOT NULL, due_date DATE NOT NULL, status ENUM('Pending','Completed','Overdue') NOT NULL DEFAULT 'Pending', CONSTRAINT fk_schedule_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id), CONSTRAINT fk_schedule_user FOREIGN KEY (assigned_to) REFERENCES users(user_id) ON DELETE SET NULL)");
                s.executeUpdate("CREATE TABLE IF NOT EXISTS maintenance_record (record_id INT AUTO_INCREMENT PRIMARY KEY, equipment_id INT NOT NULL, schedule_id INT NULL, technician_id INT NULL, maintenance_date DATE NOT NULL, work_done TEXT NOT NULL, remarks TEXT, CONSTRAINT fk_record_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id), CONSTRAINT fk_record_schedule FOREIGN KEY (schedule_id) REFERENCES maintenance_schedule(schedule_id) ON DELETE SET NULL, CONSTRAINT fk_record_technician FOREIGN KEY (technician_id) REFERENCES users(user_id) ON DELETE SET NULL)");
                s.executeUpdate("CREATE TABLE IF NOT EXISTS equipment_parts (part_id INT AUTO_INCREMENT PRIMARY KEY, equipment_id INT NOT NULL, part_name VARCHAR(100) NOT NULL, quantity INT NOT NULL DEFAULT 0, `condition` VARCHAR(60) NOT NULL DEFAULT 'Good', CONSTRAINT fk_parts_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id))");
                try (PreparedStatement p = c.prepareStatement("INSERT IGNORE INTO users(name,username,password_hash,role,status) VALUES(?,?,?,?, 'Active')")) {
                    p.setString(1, "Project Administrator"); p.setString(2, "admin"); p.setString(3, hash("admin123")); p.setString(4, "Administrator"); p.executeUpdate();
                }
            }
        }
    }

    static class LoginFrame extends JFrame {
        JTextField username = new JTextField("admin", 18); JPasswordField password = new JPasswordField("admin123", 18);
        LoginFrame() {
            setTitle("Solar Equipment Maintenance Tracker - Login"); setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(430, 260); setLocationRelativeTo(null);
            JPanel root = new JPanel(new BorderLayout(10,10)); root.setBorder(BorderFactory.createEmptyBorder(20,25,20,25));
            JLabel title = new JLabel("SOLAR EQUIPMENT MAINTENANCE TRACKER", SwingConstants.CENTER); title.setFont(new Font("Arial", Font.BOLD, 16));
            JPanel form = new JPanel(new GridLayout(2,2,8,10)); form.add(new JLabel("Username")); form.add(username); form.add(new JLabel("Password")); form.add(password);
            JButton login = new JButton("Login"); login.addActionListener(e -> doLogin()); password.addActionListener(e -> doLogin());
            JLabel hint = new JLabel("First login: admin / admin123", SwingConstants.CENTER);
            root.add(title, BorderLayout.NORTH); root.add(form, BorderLayout.CENTER); JPanel bottom = new JPanel(new BorderLayout()); bottom.add(login, BorderLayout.NORTH); bottom.add(hint, BorderLayout.SOUTH); root.add(bottom, BorderLayout.SOUTH); setContentPane(root);
        }
        void doLogin() {
            try (Connection c = connect(); PreparedStatement p = c.prepareStatement("SELECT user_id,name,role FROM users WHERE username=? AND password_hash=? AND status='Active'")) {
                p.setString(1, username.getText().trim()); p.setString(2, hash(new String(password.getPassword())));
                try (ResultSet r = p.executeQuery()) { if (r.next()) { int id=r.getInt("user_id"); String name=r.getString("name"), role=r.getString("role"); dispose(); new MainFrame(id,name,role).setVisible(true); } else showError("Login failed. Check username/password and ensure the user is Active."); }
            } catch (Exception ex) { showError(ex.getMessage()); }
        }
    }

    record Column(String name, String label, String type) {}
    static class TableSpec {
        final String table, title, pk; final java.util.List<Column> columns;
        TableSpec(String table, String title, String pk, Column... columns) { this.table=table; this.title=title; this.pk=pk; this.columns=java.util.List.of(columns); }
    }

    static class MainFrame extends JFrame {
        final int userId; final String role;
        MainFrame(int userId, String name, String role) {
            this.userId=userId; this.role=role; setTitle("Solar Equipment Maintenance Tracker"); setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(1100,700); setLocationRelativeTo(null);
            JPanel top = new JPanel(new BorderLayout()); top.setBorder(BorderFactory.createEmptyBorder(12,16,12,16)); JLabel heading=new JLabel("Solar Equipment Maintenance Tracker"); heading.setFont(new Font("Arial",Font.BOLD,22)); top.add(heading,BorderLayout.WEST); top.add(new JLabel("Signed in: "+name+" ("+role+")"),BorderLayout.EAST); add(top,BorderLayout.NORTH);
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Dashboard", dashboard());
            tabs.addTab("Equipment", new DataPanel(new TableSpec("equipment","Equipment","equipment_id", new Column("equipment_code","Equipment Code","text"),new Column("equipment_type","Equipment Type","text"),new Column("location","Location","text"),new Column("installation_date","Installation Date (YYYY-MM-DD)","date"),new Column("status","Status","text"))));
            tabs.addTab("Maintenance Schedule", new DataPanel(new TableSpec("maintenance_schedule","Maintenance Schedule","schedule_id",new Column("equipment_id","Equipment ID","number"),new Column("assigned_to","Assigned User ID (optional)","nullableNumber"),new Column("maintenance_type","Maintenance Type","text"),new Column("due_date","Due Date (YYYY-MM-DD)","date"),new Column("status","Status","text"))));
            tabs.addTab("Maintenance Records", new DataPanel(new TableSpec("maintenance_record","Maintenance Records","record_id",new Column("equipment_id","Equipment ID","number"),new Column("schedule_id","Schedule ID (optional)","nullableNumber"),new Column("technician_id","Technician User ID (optional)","nullableNumber"),new Column("maintenance_date","Maintenance Date (YYYY-MM-DD)","date"),new Column("work_done","Work Done","text"),new Column("remarks","Remarks","text"))));
            tabs.addTab("Equipment Parts", new DataPanel(new TableSpec("equipment_parts","Equipment Parts","part_id",new Column("equipment_id","Equipment ID","number"),new Column("part_name","Part Name","text"),new Column("quantity","Quantity","number"),new Column("condition","Condition","text"))));
            if ("Administrator".equals(role)) tabs.addTab("Users",new DataPanel(new TableSpec("users","Users","user_id",new Column("name","Full Name","text"),new Column("username","Username","text"),new Column("password_hash","Password (set on create)","password"),new Column("role","Role (Administrator/Maintenance Staff/Viewer)","text"),new Column("status","Status (Active/Inactive)","text"))));
            add(tabs,BorderLayout.CENTER);
        }
        JPanel dashboard() {
            JPanel p=new JPanel(new BorderLayout(10,10)); p.setBorder(BorderFactory.createEmptyBorder(20,20,20,20)); JLabel welcome=new JLabel("Welcome! Track solar equipment, planned maintenance, completed work and spare parts."); welcome.setFont(new Font("Arial",Font.PLAIN,16)); p.add(welcome,BorderLayout.NORTH);
            JPanel cards=new JPanel(new GridLayout(2,2,12,12)); addCount(cards,"Total Equipment","SELECT COUNT(*) FROM equipment"); addCount(cards,"Pending Maintenance","SELECT COUNT(*) FROM maintenance_schedule WHERE status='Pending'"); addCount(cards,"Overdue Maintenance","SELECT COUNT(*) FROM maintenance_schedule WHERE status='Overdue' OR (status='Pending' AND due_date<CURDATE())"); addCount(cards,"Parts Entries","SELECT COUNT(*) FROM equipment_parts"); p.add(cards,BorderLayout.CENTER);
            JTextArea note=new JTextArea("Project USP: Solar Equipment Health Passport + Preventive-Maintenance Priority.\n\nSuggested workflow:\n1. Add equipment in Equipment tab.\n2. Add a maintenance schedule using its Equipment ID.\n3. Record completed work in Maintenance Records.\n4. Track replacement parts in Equipment Parts.\n\nDate format: YYYY-MM-DD. IDs can be seen in the tables. Direct IoT/sensor integration is outside version 1 scope."); note.setEditable(false); note.setBackground(p.getBackground()); p.add(note,BorderLayout.SOUTH); return p;
        }
        void addCount(JPanel parent,String label,String sql) { JPanel card=new JPanel(new BorderLayout()); card.setBorder(BorderFactory.createTitledBorder(label)); JLabel value=new JLabel("—",SwingConstants.CENTER); value.setFont(new Font("Arial",Font.BOLD,28)); card.add(value); JButton refresh=new JButton("Refresh"); refresh.addActionListener(e->{try(Connection c=connect();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();value.setText(r.getString(1));}catch(Exception ex){showError(ex.getMessage());}}); refresh.doClick(); card.add(refresh,BorderLayout.SOUTH); parent.add(card); }
    }

    static class DataPanel extends JPanel {
        final TableSpec spec; final DefaultTableModel model=new DefaultTableModel(); final JTable table=new JTable(model); final LinkedHashMap<Column,JComponent> inputs=new LinkedHashMap<>();
        DataPanel(TableSpec spec) {
            this.spec=spec; setLayout(new BorderLayout(8,8)); JPanel form=new JPanel(new GridLayout(0,2,8,6)); form.setBorder(BorderFactory.createTitledBorder("Add new: "+spec.title));
            for(Column col:spec.columns){form.add(new JLabel(col.label)); JComponent field=col.type.equals("password")?new JPasswordField():new JTextField(col.type.equals("date")?LocalDate.now().toString():"",18); inputs.put(col,field); form.add(field);}
            JButton add=new JButton("Add Record"), refresh=new JButton("Refresh Table"), delete=new JButton("Delete Selected"); add.addActionListener(e->insert()); refresh.addActionListener(e->load()); delete.addActionListener(e->delete());
            JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT)); buttons.add(add);buttons.add(refresh);buttons.add(delete);
            JPanel north=new JPanel(new BorderLayout());north.add(form,BorderLayout.CENTER);north.add(buttons,BorderLayout.SOUTH);add(north,BorderLayout.NORTH);add(new JScrollPane(table),BorderLayout.CENTER); load();
        }
        void load(){try(Connection c=connect();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM `"+spec.table+"` ORDER BY `"+spec.pk+"` DESC")){model.setRowCount(0);model.setColumnCount(0);ResultSetMetaData md=r.getMetaData();for(int i=1;i<=md.getColumnCount();i++)model.addColumn(md.getColumnName(i));while(r.next()){Object[] row=new Object[md.getColumnCount()];for(int i=1;i<=row.length;i++)row[i-1]=r.getObject(i);model.addRow(row);}}catch(Exception ex){showError("Load failed: "+ex.getMessage());}}
        String value(Column col,JComponent component) { if(component instanceof JPasswordField pf) return new String(pf.getPassword()).trim(); return ((JTextField)component).getText().trim(); }
        void insert(){
            try(Connection c=connect()){
                java.util.List<String> names=new ArrayList<>();java.util.List<String> marks=new ArrayList<>();java.util.List<String> vals=new ArrayList<>();
                for(var entry:inputs.entrySet()){Column col=entry.getKey();String v=value(col,entry.getValue());if(col.type.equals("password")){if(v.isEmpty())throw new IllegalArgumentException("Password is required for a new user.");v=hash(v);}if(v.isEmpty()&&col.type.equals("nullableNumber"))v=null;if(v.isEmpty()&&col.type.equals("date"))v=null;if(v.isEmpty()&&v==null){} names.add("`"+col.name+"`");marks.add("?");vals.add(v);}
                String sql="INSERT INTO `"+spec.table+"` ("+String.join(",",names)+") VALUES ("+String.join(",",marks)+")";
                try(PreparedStatement p=c.prepareStatement(sql)){for(int i=0;i<vals.size();i++){String v=vals.get(i);Column col=(Column)inputs.keySet().toArray()[i];if(v==null)p.setNull(i+1, col.type.equals("date")?Types.DATE:Types.INTEGER);else if(col.type.equals("number")||col.type.equals("nullableNumber"))p.setInt(i+1,Integer.parseInt(v));else if(col.type.equals("date"))p.setDate(i+1,java.sql.Date.valueOf(v));else p.setString(i+1,v);}p.executeUpdate();}
                JOptionPane.showMessageDialog(this,"Record added successfully.");for(var entry:inputs.entrySet()){if(entry.getValue() instanceof JTextField tf){if(entry.getKey().type.equals("date"))tf.setText(LocalDate.now().toString());else tf.setText("");}else if(entry.getValue() instanceof JPasswordField pf)pf.setText("");}load();
            }catch(Exception ex){showError("Could not add record. Check required fields, date format, foreign-key IDs and duplicate values.\n\n"+ex.getMessage());}
        }
        void delete(){int row=table.getSelectedRow();if(row<0){showError("Select a row in the table first.");return;}Object id=model.getValueAt(table.convertRowIndexToModel(row),0);if(JOptionPane.showConfirmDialog(this,"Delete record ID "+id+"?","Confirm delete",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;try(Connection c=connect();PreparedStatement p=c.prepareStatement("DELETE FROM `"+spec.table+"` WHERE `"+spec.pk+"`=?")){p.setObject(1,id);p.executeUpdate();load();}catch(Exception ex){showError("Delete failed. Other records may refer to this record.\n"+ex.getMessage());}}
    }
}
