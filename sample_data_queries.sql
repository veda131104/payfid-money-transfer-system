-- ==========================================
-- SAMPLE DATA FOR BANK_DETAILS (25 ROWS)
-- ==========================================
INSERT INTO bank_details (account_number, bank_name, ifsc_code, branch_name, address, email, contact, user_name, credit_card_number, cvv, expiry_date, upi_id, pin, created_on) VALUES
('100000000001', 'HDFC Bank', 'HDFC0001234', 'Mumbai Main', 'Marine Drive, Mumbai', 'amit.sharma@payfid.com', '9876543210', 'amit_sharma', '4111222233334444', '123', '12/28', 'amit@fihdfc', '1234', NOW()),
('100000000002', 'ICICI Bank', 'ICIC0005678', 'Bangalore Koramangala', '100ft Road, Bangalore', 'priya.nair@payfid.com', '9823456789', 'priya_nair', '4222333344445555', '456', '06/27', 'priya@icici', '5678', NOW()),
('100000000003', 'State Bank of India', 'SBIN0009012', 'Delhi Connaught Place', 'CP, New Delhi', 'rahul.verma@payfid.com', '9811223344', 'rahul_verma', '4333444455556666', '789', '09/26', 'rahul@oksbi', '9012', NOW()),
('100000000004', 'Axis Bank', 'UTIB0003456', 'Pune Hinjewadi', 'IT Park, Pune', 'sneha.patil@payfid.com', '9844556677', 'sneha_patil', '4444555566667777', '321', '03/29', 'sneha@axisbank', '3456', NOW()),
('100000000005', 'HDFC Bank', 'HDFC0001234', 'Chennai Adyar', 'Adyar Road, Chennai', 'vikram.singh@payfid.com', '9855667788', 'vikram_singh', '4555666677778888', '654', '11/27', 'vikram@fihdfc', '7890', NOW()),
('100000000006', 'ICICI Bank', 'ICIC0005678', 'Hyderabad Gachibowli', 'Gachibowli, Hyderabad', 'ananya.das@payfid.com', '9866778899', 'ananya_das', '4666777788889999', '987', '08/30', 'ananya@icici', '1122', NOW()),
('100000000007', 'Axis Bank', 'UTIB0003456', 'Kolkata Salt Lake', 'Sector V, Kolkata', 'arjun.roy@payfid.com', '9877889900', 'arjun_roy', '4777888899990000', '213', '01/26', 'arjun@axisbank', '3344', NOW()),
('100000000008', 'State Bank of India', 'SBIN0009012', 'Ahmedabad SG Road', 'SG Road, Ahmedabad', 'kavita.mehta@payfid.com', '9888990011', 'kavita_mehta', '4888999900001111', '543', '05/28', 'kavita@oksbi', '5566', NOW()),
('100000000009', 'HDFC Bank', 'HDFC0001234', 'Jaipur C-Scheme', 'Civil Lines, Jaipur', 'rohan.gupta@payfid.com', '9899001122', 'rohan_gupta', '4999000011112222', '876', '10/26', 'rohan@fihdfc', '7788', NOW()),
('100000000010', 'ICICI Bank', 'ICIC0005678', 'Indore Vijay Nagar', 'AB Road, Indore', 'isha.khanna@payfid.com', '9800112233', 'isha_khanna', '5000111122223333', '102', '07/27', 'isha@icici', '9900', NOW()),
('100000000011', 'Axis Bank', 'UTIB0003456', 'Lucknow Gomti Nagar', 'Picup Bhawan, Lucknow', 'rishi.mishra@payfid.com', '9812345678', 'rishi_mishra', '5111222233334444', '304', '04/29', 'rishi@axisbank', '1256', NOW()),
('100000000012', 'State Bank of India', 'SBIN0009012', 'Bhopal MP Nagar', 'MP Nagar, Bhopal', 'tanvi.jain@payfid.com', '9823451234', 'tanvi_jain', '5222333344445555', '506', '12/26', 'tanvi@oksbi', '3478', NOW()),
('100000000013', 'HDFC Bank', 'HDFC0001234', 'Chandigarh Sec-17', 'Sector 17, Chandigarh', 'sahil.taneja@payfid.com', '9834567890', 'sahil_taneja', '5333444455556666', '708', '11/30', 'sahil@fihdfc', '5690', NOW()),
('100000000014', 'ICICI Bank', 'ICIC0005678', 'Nagpur Sitabuldi', 'Sitabuldi, Nagpur', 'riya.deshmukh@payfid.com', '9845678901', 'riya_deshmukh', '5444555566667777', '910', '02/27', 'riya@icici', '7812', NOW()),
('100000000015', 'Axis Bank', 'UTIB0003456', 'Surat Varachha', 'Varachha, Surat', 'manish.patel@payfid.com', '9856789012', 'manish_patel', '5555666677778888', '121', '01/28', 'manish@axisbank', '9034', NOW()),
('100000000016', 'State Bank of India', 'SBIN0009012', 'Patna Gandhi Maidan', 'Bank Road, Patna', 'sonia.sinha@payfid.com', '9867890123', 'sonia_sinha', '5666777788889999', '323', '06/29', 'sonia@oksbi', '1201', NOW()),
('100000000017', 'HDFC Bank', 'HDFC0001234', 'Kochi MG Road', 'Thevera, Kochi', 'karthik.menon@payfid.com', '9878901234', 'karthik_menon', '5777888899990000', '525', '10/26', 'karthik@fihdfc', '3412', NOW()),
('100000000018', 'ICICI Bank', 'ICIC0005678', 'Madurai Simmakkal', 'Simmakkal, Madurai', 'divya.raj@payfid.com', '9889012345', 'divya_raj', '5888999900001111', '727', '03/27', 'divya@icici', '5623', NOW()),
('100000000019', 'Axis Bank', 'UTIB0003456', 'Vishakhapatnam MVP', 'MVP Colony, Vizag', 'pavan.kumar@payfid.com', '9890123456', 'pavan_kumar', '5999000011112222', '929', '09/30', 'pavan@axisbank', '7834', NOW()),
('100000000020', 'State Bank of India', 'SBIN0009012', 'Guwahati GS Road', 'GS Road, Guwahati', 'megha.barua@payfid.com', '9801234567', 'megha_barua', '6000111122223333', '131', '05/27', 'megha@oksbi', '9045', NOW()),
('100000000021', 'HDFC Bank', 'HDFC0001234', 'Guwahati GS Road', 'GS Road, Guwahati', 'megha.barua2@payfid.com', '9801234568', 'megha_barua2', '6000111122223334', '132', '05/28', 'megha2@fihdfc', '9046', NOW()),
('100000000022', 'ICICI Bank', 'ICIC0005678', 'Varanasi Sigra', 'Sigra, Varanasi', 'ankit.rai@payfid.com', '9811112222', 'ankit_rai', '6111222233334444', '414', '08/29', 'ankit@icici', '1278', NOW()),
('100000000023', 'Axis Bank', 'UTIB0003456', 'Amritsar Golden Temple', 'Heritage Walk, Amritsar', 'punit.sandhu@payfid.com', '9822223333', 'punit_sandhu', '6222333344445555', '616', '12/27', 'punit@axisbank', '3489', NOW()),
('100000000024', 'State Bank of India', 'SBIN0009012', 'Thiruvananthapuram MG', 'Statue, Trivandrum', 'anjana.n@payfid.com', '9833334444', 'anjana_n', '6333444455556666', '818', '07/26', 'anjana@oksbi', '5601', NOW()),
('100000000025', 'HDFC Bank', 'HDFC0001234', 'Gurugram Cyber Hub', 'Cyber City, Gurgaon', 'rahul.dua@payfid.com', '9844445555', 'rahul_dua', '6444555566667777', '111', '10/30', 'rahuldua@fihdfc', '7812', NOW());

-- ==========================================
-- SAMPLE DATA FOR ACCOUNTS (25 ROWS)
-- ==========================================
INSERT INTO accounts (account_number, holder_name, balance, status, created_on) VALUES
('100000000001', 'Amit Sharma', 15000.50, 'ACTIVE', NOW()),
('100000000002', 'Priya Nair', 22400.00, 'ACTIVE', NOW()),
('100000000003', 'Rahul Verma', 8900.25, 'ACTIVE', NOW()),
('100000000004', 'Sneha Patil', 12150.75, 'ACTIVE', NOW()),
('100000000005', 'Vikram Singh', 4500.00, 'ACTIVE', NOW()),
('100000000006', 'Ananya Das', 31200.10, 'ACTIVE', NOW()),
('100000000007', 'Arjun Roy', 6700.50, 'ACTIVE', NOW()),
('100000000008', 'Kavita Mehta', 14300.20, 'ACTIVE', NOW()),
('100000000009', 'Rohan Gupta', 9200.00, 'ACTIVE', NOW()),
('100000000010', 'Isha Khanna', 18600.80, 'ACTIVE', NOW()),
('100000000011', 'Rishi Mishra', 5400.30, 'ACTIVE', NOW()),
('100000000012', 'Tanvi Jain', 21100.40, 'ACTIVE', NOW()),
('100000000013', 'Sahil Taneja', 13200.00, 'ACTIVE', NOW()),
('100000000014', 'Riya Deshmukh', 7600.25, 'ACTIVE', NOW()),
('100000000015', 'Manish Patel', 10500.60, 'ACTIVE', NOW()),
('100000000016', 'Sonia Sinha', 28400.15, 'ACTIVE', NOW()),
('100000000017', 'Karthik Menon', 16200.00, 'ACTIVE', NOW()),
('100000000018', 'Divya Raj', 5700.90, 'ACTIVE', NOW()),
('100000000019', 'Pavan Kumar', 11900.45, 'ACTIVE', NOW()),
('100000000020', 'Megha Barua', 8800.00, 'ACTIVE', NOW()),
('100000000021', 'Megha Barua 2', 14500.50, 'ACTIVE', NOW()),
('100000000022', 'Ankit Rai', 20200.00, 'ACTIVE', NOW()),
('100000000023', 'Punit Sandhu', 7400.35, 'ACTIVE', NOW()),
('100000000024', 'Anjana N', 15150.25, 'ACTIVE', NOW()),
('100000000025', 'Rahul Dua', 9300.00, 'ACTIVE', NOW());

-- ==========================================
-- SAMPLE DATA FOR TRANSACTION_LOGS (25 ROWS)
-- ==========================================
-- Note: Assuming IDs in accounts table start from 1 to 25
INSERT INTO transaction_logs (from_account_id, to_account_id, amount, type, status, transaction_date, description, from_account_balance_before, from_account_balance_after, to_account_balance_before, to_account_balance_after, idempotency_key) VALUES
(1, 2, 500.00, 'TRANSFER', 'SUCCESS', NOW(), 'Rent payment', 15500.50, 15000.50, 21900.00, 22400.00, 'IDEM-001'),
(2, 3, 1200.00, 'TRANSFER', 'SUCCESS', NOW(), 'Grocery bill', 23600.00, 22400.00, 7700.25, 8900.25, 'IDEM-002'),
(4, 1, 250.75, 'TRANSFER', 'SUCCESS', NOW(), 'Lunch split', 12401.50, 12150.75, 14749.75, 15000.50, 'IDEM-003'),
(5, 5, 1000.00, 'CREDIT', 'SUCCESS', NOW(), 'ATM Deposit', 3500.00, 4500.00, 3500.00, 4500.00, 'IDEM-004'),
(6, 6, 200.00, 'DEBIT', 'SUCCESS', NOW(), 'Cash Withdrawal', 31400.10, 31200.10, 31400.10, 31200.10, 'IDEM-005'),
(7, 10, 1500.00, 'TRANSFER', 'SUCCESS', NOW(), 'Laptop accessory', 8200.50, 6700.50, 17100.80, 18600.80, 'IDEM-006'),
(8, 9, 300.20, 'TRANSFER', 'SUCCESS', NOW(), 'Book purchase', 14600.40, 14300.20, 8899.80, 9200.00, 'IDEM-007'),
(11, 12, 450.00, 'TRANSFER', 'SUCCESS', NOW(), 'Dinner payment', 5850.30, 5400.30, 20650.40, 21100.40, 'IDEM-008'),
(13, 14, 5000.00, 'TRANSFER', 'SUCCESS', NOW(), 'Gift to sister', 18200.00, 13200.00, 2600.25, 7600.25, 'IDEM-009'),
(15, 16, 1200.00, 'TRANSFER', 'SUCCESS', NOW(), 'Internet bill', 11700.60, 10500.60, 27200.15, 28400.15, 'IDEM-010'),
(17, 17, 500.00, 'CREDIT', 'SUCCESS', NOW(), 'Online Refund', 15700.00, 16200.00, 15700.00, 16200.00, 'IDEM-011'),
(18, 19, 800.00, 'TRANSFER', 'SUCCESS', NOW(), 'Medical consultation', 6500.90, 5700.90, 11100.45, 11900.45, 'IDEM-012'),
(20, 21, 600.00, 'TRANSFER', 'SUCCESS', NOW(), 'Cable TV bill', 9400.00, 8800.00, 13900.50, 14500.50, 'IDEM-013'),
(22, 23, 2000.00, 'TRANSFER', 'SUCCESS', NOW(), 'Phone bill', 22200.00, 20200.00, 5400.35, 7400.35, 'IDEM-014'),
(24, 25, 300.00, 'TRANSFER', 'SUCCESS', NOW(), 'OTT Subscription', 15450.25, 15150.25, 9000.00, 9300.00, 'IDEM-015'),
(1, 3, 100.00, 'TRANSFER', 'FAILED', NOW(), 'Low Balance Test', 15000.50, 15000.50, 8900.25, 8900.25, 'IDEM-016'),
(2, 4, 150.00, 'TRANSFER', 'PENDING', NOW(), 'Awaiting authorization', 22400.00, 22400.00, 12150.75, 12150.75, 'IDEM-017'),
(3, 1, 1000.00, 'TRANSFER', 'SUCCESS', NOW(), 'Repayment', 9900.25, 8900.25, 14000.50, 15000.50, 'IDEM-018'),
(4, 5, 200.00, 'TRANSFER', 'SUCCESS', NOW(), 'Charity donation', 12350.75, 12150.75, 4300.00, 4500.00, 'IDEM-019'),
(6, 7, 500.00, 'TRANSFER', 'SUCCESS', NOW(), 'Cab fare', 31700.10, 31200.10, 6200.50, 6700.50, 'IDEM-020'),
(8, 10, 120.50, 'TRANSFER', 'SUCCESS', NOW(), 'Stationery purchase', 14420.70, 14300.20, 18480.30, 18600.80, 'IDEM-021'),
(12, 11, 230.00, 'TRANSFER', 'SUCCESS', NOW(), 'Coffee split', 21330.40, 21100.40, 5170.30, 5400.30, 'IDEM-022'),
(14, 13, 850.00, 'TRANSFER', 'SUCCESS', NOW(), 'Dinner Split', 8450.25, 7600.25, 12350.00, 13200.00, 'IDEM-023'),
(16, 15, 150.75, 'TRANSFER', 'SUCCESS', NOW(), 'Snacks split', 28550.90, 28400.15, 10349.85, 10500.60, 'IDEM-024'),
(19, 18, 400.00, 'TRANSFER', 'SUCCESS', NOW(), 'Bus fare', 12300.45, 11900.45, 5300.90, 5700.90, 'IDEM-025');
