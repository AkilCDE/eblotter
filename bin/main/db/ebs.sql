-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Apr 14, 2026 at 06:49 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `ebs`
--

-- --------------------------------------------------------

--
-- Table structure for table `blotter`
--

CREATE TABLE `blotter` (
  `blotter_id` int(100) NOT NULL,
  `complainant` varchar(100) NOT NULL,
  `Cmplnt_address` varchar(100) NOT NULL,
  `Respondent` varchar(100) NOT NULL,
  `complt_type` varchar(100) NOT NULL,
  `description` varchar(1000) NOT NULL,
  `date` date NOT NULL,
  `status` enum('pending','resolved') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `blotter`
--

INSERT INTO `blotter` (`blotter_id`, `complainant`, `Cmplnt_address`, `Respondent`, `complt_type`, `description`, `date`, `status`) VALUES
(1, 'aluk', '', 'akil', '', '', '2026-04-13', 'resolved'),
(2, 'alex', '', 'jerone', '', '', '2026-04-13', 'resolved'),
(3, 'adfsf', '', 'asad', '', '', '2026-04-13', 'resolved'),
(4, 'asss', '', 'asss', '', '', '2026-04-13', 'resolved'),
(5, 'asas', 'adto', 'asas', '', '', '2026-04-13', 'pending'),
(6, 'asas', '', 'asas', '', '', '2026-04-13', 'pending'),
(7, 'asdf', '', 'asd', '', '', '2026-04-13', 'pending'),
(8, 'asdfds', '', 'asddsa', '', '', '2026-04-13', 'pending'),
(9, 'asdfg', '', 'asdfgsasdfg', '', '', '2026-04-13', 'pending'),
(10, 'add', 'wewewe', 'sasas', 'Property Damage', 'aaqeweesscscsd', '2026-04-13', 'pending');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `user_id` int(50) NOT NULL,
  `username` text NOT NULL,
  `password` text NOT NULL,
  `full_name` text NOT NULL,
  `role` enum('secretary','captain','kagawad') NOT NULL,
  `created_at` text NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`user_id`, `username`, `password`, `full_name`, `role`, `created_at`) VALUES
(1, 'captain', '112233', 'Toniti', 'captain', '2026-04-11 21:49:11'),
(2, 'secretary', '112233', 'Deogracia', 'secretary', '2026-04-15 00:25:22'),
(3, 'kagawad', '112233', 'Elijane', 'kagawad', '2026-04-15 00:25:22');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `blotter`
--
ALTER TABLE `blotter`
  ADD PRIMARY KEY (`blotter_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `username` (`username`) USING HASH;

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `blotter`
--
ALTER TABLE `blotter`
  MODIFY `blotter_id` int(100) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `user_id` int(50) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
