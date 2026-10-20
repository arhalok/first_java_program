CREATE DATABASE IF NOT EXISTS solar_maintenance;
USE solar_maintenance;
CREATE TABLE IF NOT EXISTS users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  username VARCHAR(60) NOT NULL UNIQUE,
  password_hash VARCHAR(64) NOT NULL,
  role ENUM('Administrator','Maintenance Staff','Viewer') NOT NULL DEFAULT 'Viewer',
  status ENUM('Active','Inactive') NOT NULL DEFAULT 'Active'
);
CREATE TABLE IF NOT EXISTS equipment (
  equipment_id INT AUTO_INCREMENT PRIMARY KEY,
  equipment_code VARCHAR(40) NOT NULL UNIQUE,
  equipment_type VARCHAR(100) NOT NULL,
  location VARCHAR(150) NOT NULL,
  installation_date DATE NULL,
  status ENUM('Active','Under Maintenance','Inactive') NOT NULL DEFAULT 'Active'
);
CREATE TABLE IF NOT EXISTS maintenance_schedule (
  schedule_id INT AUTO_INCREMENT PRIMARY KEY,
  equipment_id INT NOT NULL,
  assigned_to INT NULL,
  maintenance_type VARCHAR(100) NOT NULL,
  due_date DATE NOT NULL,
  status ENUM('Pending','Completed','Overdue') NOT NULL DEFAULT 'Pending',
  CONSTRAINT fk_schedule_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id),
  CONSTRAINT fk_schedule_user FOREIGN KEY (assigned_to) REFERENCES users(user_id) ON DELETE SET NULL
);
CREATE TABLE IF NOT EXISTS maintenance_record (
  record_id INT AUTO_INCREMENT PRIMARY KEY,
  equipment_id INT NOT NULL,
  schedule_id INT NULL,
  technician_id INT NULL,
  maintenance_date DATE NOT NULL,
  work_done TEXT NOT NULL,
  remarks TEXT,
  CONSTRAINT fk_record_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id),
  CONSTRAINT fk_record_schedule FOREIGN KEY (schedule_id) REFERENCES maintenance_schedule(schedule_id) ON DELETE SET NULL,
  CONSTRAINT fk_record_technician FOREIGN KEY (technician_id) REFERENCES users(user_id) ON DELETE SET NULL
);
CREATE TABLE IF NOT EXISTS equipment_parts (
  part_id INT AUTO_INCREMENT PRIMARY KEY,
  equipment_id INT NOT NULL,
  part_name VARCHAR(100) NOT NULL,
  quantity INT NOT NULL DEFAULT 0,
  `condition` VARCHAR(60) NOT NULL DEFAULT 'Good',
  CONSTRAINT fk_parts_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id)
);
