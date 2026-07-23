-- ============================================
-- 直销系统测试数据生成脚本（幂等版）
-- 日期：2026-07-18
-- 说明：在 init.sql 执行完毕后执行，可重复执行不报错
-- 如需清空旧数据重来，取消下面 DELETE 语句的注释
-- ============================================

-- ============================================
-- 0. 清空旧测试数据（按依赖顺序倒序删除，可选）
-- ============================================
-- DELETE FROM ds_sys_log;
-- DELETE FROM ds_payment_order;
-- DELETE FROM ds_file_record;
-- DELETE FROM ds_member_social;
-- DELETE FROM ds_withdraw;
-- DELETE FROM ds_wallet_log;
-- DELETE FROM ds_bonus_record;
-- DELETE FROM ds_order_bonus;
-- DELETE FROM ds_order_item;
-- DELETE FROM ds_order;
-- DELETE FROM ds_member_path;
-- DELETE FROM ds_member_log;
-- DELETE FROM ds_member;
-- DELETE FROM ds_product_sku;
-- DELETE FROM ds_product;
-- DELETE FROM ds_product_category;

-- ============================================
-- 1. 商品分类
-- ============================================
INSERT INTO ds_product_category (id, parent_id, name, sort_order, status) VALUES
(1, 0, '健康食品', 1, 1),
(2, 0, '个人护理', 2, 1),
(3, 0, '家居生活', 3, 1),
(4, 1, '蛋白粉', 1, 1),
(5, 1, '维生素', 2, 1),
(6, 1, '代餐食品', 3, 1),
(7, 2, '护肤品', 1, 1),
(8, 2, '洗发护发', 2, 1),
(9, 3, '厨房用品', 1, 1),
(10, 3, '床上用品', 2, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), parent_id = VALUES(parent_id), sort_order = VALUES(sort_order);

-- ============================================
-- 2. 商品主表
-- ============================================
INSERT INTO ds_product (id, product_no, category_id, name, subtitle, main_image, retail_price, member_price, pv, stock, sales_count, is_recommend, is_new, status) VALUES
(1, 'P202607180001', 4, '乳清蛋白粉 500g', '进口乳清蛋白，健身必备', 'https://img.example.com/product/p1.jpg', 298.00, 268.00, 50.00, 500, 0, 1, 1, 1),
(2, 'P202607180002', 4, '植物蛋白粉 400g', '纯植物提取，素食者首选', 'https://img.example.com/product/p2.jpg', 258.00, 228.00, 40.00, 300, 0, 0, 1, 1),
(3, 'P202607180003', 5, '复合维生素片 120粒', '每日一片，全面补充', 'https://img.example.com/product/p3.jpg', 198.00, 168.00, 30.00, 800, 0, 1, 0, 1),
(4, 'P202607180004', 5, '维生素C咀嚼片 100粒', '高含量VC，增强免疫力', 'https://img.example.com/product/p4.jpg', 128.00, 108.00, 20.00, 1000, 0, 0, 0, 1),
(5, 'P202607180005', 6, '代餐奶昔 10袋装', '饱腹代餐，健康减脂', 'https://img.example.com/product/p5.jpg', 358.00, 318.00, 60.00, 400, 0, 1, 1, 1),
(6, 'P202607180006', 7, '玻尿酸保湿面膜 5片装', '深层补水，水润肌肤', 'https://img.example.com/product/p6.jpg', 168.00, 148.00, 25.00, 600, 0, 0, 0, 1),
(7, 'P202607180007', 7, '氨基酸洁面乳 120ml', '温和清洁，不紧绷', 'https://img.example.com/product/p7.jpg', 138.00, 118.00, 20.00, 450, 0, 0, 0, 1),
(8, 'P202607180008', 8, '生姜洗发水 500ml', '防脱固发，强韧发根', 'https://img.example.com/product/p8.jpg', 98.00, 88.00, 15.00, 700, 0, 0, 0, 1),
(9, 'P202607180009', 9, '不粘锅三件套', '煎炒炖煮，一应俱全', 'https://img.example.com/product/p9.jpg', 599.00, 499.00, 100.00, 200, 0, 0, 0, 1),
(10, 'P202607180010', 10, '乳胶枕头', '天然乳胶，护颈助眠', 'https://img.example.com/product/p10.jpg', 399.00, 359.00, 70.00, 300, 0, 1, 0, 1)
ON DUPLICATE KEY UPDATE product_no = VALUES(product_no), name = VALUES(name), retail_price = VALUES(retail_price), member_price = VALUES(member_price), pv = VALUES(pv), stock = VALUES(stock);

-- ============================================
-- 3. 商品SKU
-- ============================================
INSERT INTO ds_product_sku (id, product_id, sku_code, spec_name, spec_value, price, stock, status) VALUES
(1, 1, 'SKU-001-VAN', '口味', '香草味', 298.00, 200, 1),
(2, 1, 'SKU-001-CHO', '口味', '巧克力味', 298.00, 180, 1),
(3, 1, 'SKU-001-STR', '口味', '草莓味', 298.00, 120, 1),
(4, 2, 'SKU-002-ORG', '口味', '原味', 258.00, 150, 1),
(5, 2, 'SKU-002-COC', '口味', '椰子味', 258.00, 150, 1),
(6, 3, 'SKU-003-120', '规格', '120粒', 198.00, 800, 1),
(7, 4, 'SKU-004-100', '规格', '100粒', 128.00, 1000, 1),
(8, 5, 'SKU-005-CHO', '口味', '巧克力味', 358.00, 200, 1),
(9, 5, 'SKU-005-STR', '口味', '草莓味', 358.00, 200, 1),
(10, 6, 'SKU-006-5P', '规格', '5片装', 168.00, 600, 1),
(11, 7, 'SKU-007-120', '规格', '120ml', 138.00, 450, 1),
(12, 8, 'SKU-008-500', '规格', '500ml', 98.00, 700, 1),
(13, 9, 'SKU-009-SET', '规格', '三件套', 599.00, 200, 1),
(14, 10, 'SKU-010-STD', '规格', '标准款', 399.00, 300, 1)
ON DUPLICATE KEY UPDATE sku_code = VALUES(sku_code), price = VALUES(price), stock = VALUES(stock);

-- ============================================
-- 4. 测试会员
-- 树形结构：
--       张三(推荐人/安置人=系统)
--      /              \
--   李四(左区)        王五(右区)
--    /    \            /    \
-- 赵六   钱七       孙八   周九
--   |      |          |      |
-- 吴十   郑十一    冯十二  陈十三
-- ============================================
INSERT INTO ds_member (id, member_no, phone, password, real_name, nickname, level_id, recommend_id, parent_id, position, ancestor_path, wallet_balance, total_pv, status, register_ip, last_login_time) VALUES
(1, 'M202607000001', '13800000001', '$2a$10$Ye2V4T1ENIO9.3SF9axbhOK2sCfh6RLNUJ5yiCvgDl3TrrHA38m3C', '张三', '顶级领袖', 5, NULL, NULL, 0, '1', 50000.00, 18000.00, 1, '127.0.0.1', '2026-07-18 08:00:00'),
(2, 'M202607000002', '13800000002', '$2a$10$Ye2V4T1ENIO9.3SF9axbhOK2sCfh6RLNUJ5yiCvgDl3TrrHA38m3C', '李四', '左区领袖', 4, 1, 1, 0, '1,2', 25000.00, 8000.00, 1, '127.0.0.1', '2026-07-17 15:30:00'),
(3, 'M202607000003', '13800000003', '$2a$10$Ye2V4T1ENIO9.3SF9axbhOK2sCfh6RLNUJ5yiCvgDl3TrrHA38m3C', '王五', '右区领袖', 4, 1, 1, 1, '1,3', 28000.00, 9000.00, 1, '127.0.0.1', '2026-07-18 09:00:00'),
(4, 'M202607000004', '13800000004', '$2a$10$Ye2V4T1ENIO9.3SF9axbhOK2sCfh6RLNUJ5yiCvgDl3TrrHA38m3C', '赵六', '健康达人', 3, 2, 2, 0, '1,2,4', 8000.00, 3000.00, 1, '127.0.0.1', '2026-07-16 10:00:00'),
(5, 'M202607000005', '13800000005', '$2a$10$Ye2V4T1ENIO9.3SF9axbhOK2sCfh6RLNUJ5yiCvgDl3TrrHA38m3C', '钱七', '美丽使者', 3, 2, 2, 1, '1,2,5', 6500.00, 2500.00, 1, '127.0.0.1', '2026-07-17 11:00:00'),
(6, 'M202607000006', '13800000006', '$2a$10$Ye2V4T1ENIO9.3SF9axbhOK2sCfh6RLNUJ5yiCvgDl3TrrHA38m3C', '孙八', '生活家', 2, 3, 3, 0, '1,3,6', 3500.00, 1200.00, 1, '127.0.0.1', '2026-07-15 14:00:00'),
(7, 'M202607000007', '13800000007', '$2a$10$Ye2V4T1ENIO9.3SF9axbhOK2sCfh6RLNUJ5yiCvgDl3TrrHA38m3C', '周九', '品质控', 2, 3, 3, 1, '1,3,7', 4200.00, 1500.00, 1, '127.0.0.1', '2026-07-18 07:00:00'),
(8, 'M202607000008', '13800000008', '$2a$10$Ye2V4T1ENIO9.3SF9axbhOK2sCfh6RLNUJ5yiCvgDl3TrrHA38m3C', '吴十', '新锐会员', 1, 4, 4, 0, '1,2,4,8', 1200.00, 500.00, 1, '127.0.0.1', '2026-07-14 09:00:00'),
(9, 'M202607000009', '13800000009', '$2a$10$Ye2V4T1ENIO9.3SF9axbhOK2sCfh6RLNUJ5yiCvgDl3TrrHA38m3C', '郑十一', '潜力股', 1, 5, 5, 0, '1,2,5,9', 800.00, 300.00, 1, '127.0.0.1', '2026-07-13 16:00:00'),
(10, 'M202607000010', '13800000010', '$2a$10$Ye2V4T1ENIO9.3SF9axbhOK2sCfh6RLNUJ5yiCvgDl3TrrHA38m3C', '冯十二', '新人', 1, 6, 6, 0, '1,3,6,10', 500.00, 200.00, 1, '127.0.0.1', '2026-07-16 12:00:00'),
(11, 'M202607000011', '13800000011', '$2a$10$Ye2V4T1ENIO9.3SF9axbhOK2sCfh6RLNUJ5yiCvgDl3TrrHA38m3C', '陈十三', '新人', 1, 7, 7, 0, '1,3,7,11', 300.00, 100.00, 1, '127.0.0.1', '2026-07-17 20:00:00'),
(12, 'M202607000012', '13800000012', '$2a$10$Ye2V4T1ENIO9.3SF9axbhOK2sCfh6RLNUJ5yiCvgDl3TrrHA38m3C', '褚十四', '待审核会员', 1, 1, 1, 0, '1,12', 0.00, 0.00, 2, '127.0.0.1', NULL)
ON DUPLICATE KEY UPDATE member_no = VALUES(member_no), phone = VALUES(phone), password = VALUES(password), real_name = VALUES(real_name), nickname = VALUES(nickname), level_id = VALUES(level_id), wallet_balance = VALUES(wallet_balance), total_pv = VALUES(total_pv), status = VALUES(status);
-- 密码统一为 123456

-- ============================================
-- 5. 会员关系路径表（闭包表）
-- ============================================
INSERT INTO ds_member_path (ancestor_id, descendant_id, depth) VALUES
(1, 1, 0),
(1, 2, 1), (1, 3, 1),
(1, 4, 2), (1, 5, 2), (1, 6, 2), (1, 7, 2),
(1, 8, 3), (1, 9, 3), (1, 10, 3), (1, 11, 3),
(1, 12, 1),
(2, 2, 0), (2, 4, 1), (2, 5, 1), (2, 8, 2), (2, 9, 2),
(3, 3, 0), (3, 6, 1), (3, 7, 1), (3, 10, 2), (3, 11, 2),
(4, 4, 0), (4, 8, 1),
(5, 5, 0), (5, 9, 1),
(6, 6, 0), (6, 10, 1),
(7, 7, 0), (7, 11, 1),
(8, 8, 0), (9, 9, 0), (10, 10, 0), (11, 11, 0), (12, 12, 0)
ON DUPLICATE KEY UPDATE depth = VALUES(depth);

-- ============================================
-- 6. 会员操作日志
-- ============================================
INSERT INTO ds_member_log (id, member_id, action, content, ip, create_time) VALUES
(1, 1, 'REGISTER', '会员注册成功', '127.0.0.1', '2026-06-01 08:00:00'),
(2, 1, 'LOGIN', '会员登录', '127.0.0.1', '2026-07-18 08:00:00'),
(3, 2, 'REGISTER', '会员注册成功，推荐人：张三', '127.0.0.1', '2026-06-10 10:00:00'),
(4, 2, 'LOGIN', '会员登录', '127.0.0.1', '2026-07-17 15:30:00'),
(5, 3, 'REGISTER', '会员注册成功，推荐人：张三', '127.0.0.1', '2026-06-12 14:00:00'),
(6, 4, 'REGISTER', '会员注册成功，推荐人：李四', '127.0.0.1', '2026-06-20 09:00:00'),
(7, 5, 'REGISTER', '会员注册成功，推荐人：李四', '127.0.0.1', '2026-06-22 11:00:00'),
(8, 6, 'REGISTER', '会员注册成功，推荐人：王五', '127.0.0.1', '2026-06-25 16:00:00'),
(9, 8, 'LEVEL_UP', '等级升级：普通会员 -> 银卡会员', '127.0.0.1', '2026-07-10 10:00:00'),
(10, 8, 'LEVEL_UP', '等级降级：银卡会员 -> 普通会员', '127.0.0.1', '2026-07-14 10:00:00')
ON DUPLICATE KEY UPDATE action = VALUES(action), content = VALUES(content);

-- ============================================
-- 7. 订单数据
-- ============================================
INSERT INTO ds_order (id, order_no, member_id, total_amount, pay_amount, discount_amount, total_pv, receiver_name, receiver_phone, receiver_addr, pay_type, pay_time, status, remark, create_time) VALUES
(1, 'O202607180001', 1, 298.00, 268.00, 30.00, 50.00, '张三', '13800000001', '北京市朝阳区xxx路1号', 1, '2026-07-10 10:00:00', 4, '', '2026-07-10 09:55:00'),
(2, 'O202607180002', 1, 358.00, 318.00, 40.00, 60.00, '张三', '13800000001', '北京市朝阳区xxx路1号', 1, '2026-07-15 14:00:00', 3, '', '2026-07-15 13:50:00'),
(3, 'O202607180003', 2, 198.00, 168.00, 30.00, 30.00, '李四', '13800000002', '上海市浦东新区xxx路2号', 2, '2026-07-12 09:00:00', 4, '', '2026-07-12 08:55:00'),
(4, 'O202607180004', 2, 599.00, 499.00, 100.00, 100.00, '李四', '13800000002', '上海市浦东新区xxx路2号', 1, '2026-07-16 11:00:00', 2, '已发货', '2026-07-16 10:50:00'),
(5, 'O202607180005', 3, 168.00, 148.00, 20.00, 25.00, '王五', '13800000003', '广州市天河区xxx路3号', 1, '2026-07-11 16:00:00', 4, '', '2026-07-11 15:55:00'),
(6, 'O202607180006', 3, 128.00, 108.00, 20.00, 20.00, '王五', '13800000003', '广州市天河区xxx路3号', 3, '2026-07-14 08:00:00', 4, '', '2026-07-14 07:50:00'),
(7, 'O202607180007', 4, 298.00, 268.00, 30.00, 50.00, '赵六', '13800000004', '深圳市南山区xxx路4号', 1, '2026-07-13 10:00:00', 4, '', '2026-07-13 09:55:00'),
(8, 'O202607180008', 5, 138.00, 118.00, 20.00, 20.00, '钱七', '13800000005', '杭州市西湖区xxx路5号', 1, '2026-07-14 12:00:00', 4, '', '2026-07-14 11:50:00'),
(9, 'O202607180009', 6, 399.00, 359.00, 40.00, 70.00, '孙八', '13800000006', '成都市武侯区xxx路6号', 1, '2026-07-15 09:00:00', 2, '已发货', '2026-07-15 08:50:00'),
(10, 'O202607180010', 8, 98.00, 88.00, 10.00, 15.00, '吴十', '13800000008', '武汉市洪山区xxx路8号', 1, '2026-07-16 15:00:00', 4, '', '2026-07-16 14:50:00'),
(11, 'O202607180011', 5, 258.00, 228.00, 30.00, 40.00, '钱七', '13800000005', '杭州市西湖区xxx路5号', NULL, NULL, 0, '', '2026-07-18 10:00:00'),
(12, 'O202607180012', 7, 358.00, 318.00, 40.00, 60.00, '周九', '13800000007', '南京市玄武区xxx路7号', NULL, NULL, 5, '客户主动取消', '2026-07-17 17:00:00')
ON DUPLICATE KEY UPDATE order_no = VALUES(order_no), total_amount = VALUES(total_amount), pay_amount = VALUES(pay_amount), status = VALUES(status), remark = VALUES(remark);

-- ============================================
-- 8. 订单明细
-- ============================================
INSERT INTO ds_order_item (id, order_id, product_id, product_name, product_image, sku_code, price, quantity, pv) VALUES
(1, 1, 1, '乳清蛋白粉 500g', 'https://img.example.com/product/p1.jpg', 'SKU-001-VAN', 298.00, 1, 50.00),
(2, 2, 5, '代餐奶昔 10袋装', 'https://img.example.com/product/p5.jpg', 'SKU-005-CHO', 358.00, 1, 60.00),
(3, 3, 3, '复合维生素片 120粒', 'https://img.example.com/product/p3.jpg', 'SKU-003-120', 198.00, 1, 30.00),
(4, 4, 9, '不粘锅三件套', 'https://img.example.com/product/p9.jpg', 'SKU-009-SET', 599.00, 1, 100.00),
(5, 5, 6, '玻尿酸保湿面膜 5片装', 'https://img.example.com/product/p6.jpg', 'SKU-006-5P', 168.00, 1, 25.00),
(6, 6, 4, '维生素C咀嚼片 100粒', 'https://img.example.com/product/p4.jpg', 'SKU-004-100', 128.00, 1, 20.00),
(7, 7, 1, '乳清蛋白粉 500g', 'https://img.example.com/product/p1.jpg', 'SKU-001-CHO', 298.00, 1, 50.00),
(8, 8, 7, '氨基酸洁面乳 120ml', 'https://img.example.com/product/p7.jpg', 'SKU-007-120', 138.00, 1, 20.00),
(9, 9, 10, '乳胶枕头', 'https://img.example.com/product/p10.jpg', 'SKU-010-STD', 399.00, 1, 70.00),
(10, 10, 8, '生姜洗发水 500ml', 'https://img.example.com/product/p8.jpg', 'SKU-008-500', 98.00, 1, 15.00),
(11, 11, 2, '植物蛋白粉 400g', 'https://img.example.com/product/p2.jpg', 'SKU-002-ORG', 258.00, 1, 40.00),
(12, 12, 5, '代餐奶昔 10袋装', 'https://img.example.com/product/p5.jpg', 'SKU-005-STR', 358.00, 1, 60.00)
ON DUPLICATE KEY UPDATE product_name = VALUES(product_name), price = VALUES(price), quantity = VALUES(quantity), pv = VALUES(pv);

-- ============================================
-- 9. 订单业绩归属
-- ============================================
INSERT INTO ds_order_bonus (id, order_id, member_id, relation_type, bonus_level, pv) VALUES
(1, 1, 1, 'recommend', 0, 50.00),
(2, 3, 1, 'recommend', 1, 30.00),
(3, 3, 2, 'recommend', 0, 30.00),
(4, 5, 1, 'recommend', 1, 25.00),
(5, 5, 3, 'recommend', 0, 25.00),
(6, 7, 2, 'recommend', 1, 50.00),
(7, 7, 1, 'recommend', 2, 50.00),
(8, 7, 4, 'recommend', 0, 50.00),
(9, 8, 2, 'recommend', 1, 20.00),
(10, 8, 1, 'recommend', 2, 20.00),
(11, 8, 5, 'recommend', 0, 20.00),
(12, 9, 3, 'recommend', 1, 70.00),
(13, 9, 1, 'recommend', 2, 70.00),
(14, 9, 6, 'recommend', 0, 70.00),
(15, 10, 4, 'recommend', 1, 15.00),
(16, 10, 2, 'recommend', 2, 15.00),
(17, 10, 1, 'recommend', 3, 15.00),
(18, 10, 8, 'recommend', 0, 15.00)
ON DUPLICATE KEY UPDATE pv = VALUES(pv);

-- ============================================
-- 10. 奖金结算记录
-- ============================================
INSERT INTO ds_bonus_record (id, member_id, period, bonus_type, amount, source_order_id, source_order_no, status, create_time, grant_time) VALUES
(1, 1, '2026-07', 'RECOMMEND', 350.00, NULL, NULL, 1, '2026-07-17 00:00:00', '2026-07-17 00:00:00'),
(2, 2, '2026-07', 'RECOMMEND', 200.00, NULL, NULL, 1, '2026-07-17 00:00:00', '2026-07-17 00:00:00'),
(3, 3, '2026-07', 'RECOMMEND', 150.00, NULL, NULL, 1, '2026-07-17 00:00:00', '2026-07-17 00:00:00'),
(4, 1, '2026-07', 'MATCH', 500.00, NULL, NULL, 1, '2026-07-17 00:00:00', '2026-07-17 00:00:00'),
(5, 2, '2026-07', 'MATCH', 300.00, NULL, NULL, 1, '2026-07-17 00:00:00', '2026-07-17 00:00:00'),
(6, 3, '2026-07', 'MATCH', 250.00, NULL, NULL, 1, '2026-07-17 00:00:00', '2026-07-17 00:00:00'),
(7, 1, '2026-07', 'LEADER', 1000.00, NULL, NULL, 1, '2026-07-17 00:00:00', '2026-07-17 00:00:00'),
(8, 4, '2026-07', 'RECOMMEND', 80.00, NULL, NULL, 1, '2026-07-17 00:00:00', '2026-07-17 00:00:00'),
(9, 5, '2026-07', 'RECOMMEND', 50.00, NULL, NULL, 1, '2026-07-17 00:00:00', '2026-07-17 00:00:00'),
(10, 1, '2026-07', 'RETAIL', 200.00, 1, 'O202607180001', 0, '2026-07-17 00:00:00', NULL),
(11, 2, '2026-07', 'RETAIL', 120.00, 3, 'O202607180003', 0, '2026-07-17 00:00:00', NULL),
(12, 3, '2026-07', 'RETAIL', 100.00, 5, 'O202607180005', 0, '2026-07-17 00:00:00', NULL)
ON DUPLICATE KEY UPDATE amount = VALUES(amount), status = VALUES(status), grant_time = VALUES(grant_time);

-- ============================================
-- 11. 钱包流水
-- ============================================
INSERT INTO ds_wallet_log (id, member_id, log_type, amount, balance_before, balance_after, reference_id, remark, create_time) VALUES
(1, 1, 'BONUS', 350.00, 48000.00, 48350.00, 1, '推荐奖金发放 2026-07', '2026-07-17 00:00:00'),
(2, 1, 'BONUS', 500.00, 48350.00, 48850.00, 4, '对碰奖金发放 2026-07', '2026-07-17 00:00:00'),
(3, 1, 'BONUS', 1000.00, 48850.00, 49850.00, 7, '领导奖金发放 2026-07', '2026-07-17 00:00:00'),
(4, 1, 'INCOME', 150.00, 49850.00, 50000.00, NULL, '系统充值', '2026-07-18 08:00:00'),
(5, 2, 'BONUS', 200.00, 24800.00, 25000.00, 2, '推荐奖金发放 2026-07', '2026-07-17 00:00:00'),
(6, 2, 'BONUS', 300.00, 24700.00, 24800.00, 5, '对碰奖金发放 2026-07', '2026-07-17 00:00:00'),
(7, 3, 'BONUS', 150.00, 27550.00, 27700.00, 3, '推荐奖金发放 2026-07', '2026-07-17 00:00:00'),
(8, 3, 'BONUS', 250.00, 27700.00, 27950.00, 6, '对碰奖金发放 2026-07', '2026-07-17 00:00:00'),
(9, 3, 'INCOME', 50.00, 27950.00, 28000.00, NULL, '系统充值', '2026-07-18 09:00:00'),
(10, 4, 'BONUS', 80.00, 7920.00, 8000.00, 8, '推荐奖金发放 2026-07', '2026-07-17 00:00:00'),
(11, 5, 'BONUS', 50.00, 6450.00, 6500.00, 9, '推荐奖金发放 2026-07', '2026-07-17 00:00:00'),
(12, 1, 'WITHDRAW', -2000.00, 50000.00, 48000.00, 1, '提现申请 TX202607180001', '2026-07-16 10:00:00')
ON DUPLICATE KEY UPDATE amount = VALUES(amount), balance_before = VALUES(balance_before), balance_after = VALUES(balance_after), remark = VALUES(remark);

-- ============================================
-- 12. 提现申请
-- ============================================
INSERT INTO ds_withdraw (id, member_id, withdraw_no, amount, fee, actual_amount, bank_name, bank_card, status, audit_time, grant_time, remark, create_time) VALUES
(1, 1, 'TX202607180001', 2000.00, 5.00, 1995.00, '中国工商银行', '6222***********1234', 2, '2026-07-16 12:00:00', '2026-07-16 14:00:00', '', '2026-07-16 10:00:00'),
(2, 2, 'TX202607180002', 1000.00, 2.50, 997.50, '中国建设银行', '6227***********5678', 1, '2026-07-17 09:00:00', NULL, '审核通过，待打款', '2026-07-17 08:00:00'),
(3, 3, 'TX202607180003', 500.00, 0.00, 500.00, '中国农业银行', '6228***********9012', 0, NULL, NULL, '待审核', '2026-07-18 09:30:00')
ON DUPLICATE KEY UPDATE withdraw_no = VALUES(withdraw_no), amount = VALUES(amount), status = VALUES(status), audit_time = VALUES(audit_time), grant_time = VALUES(grant_time);

-- ============================================
-- 13. 支付流水
-- ============================================
INSERT INTO ds_payment_order (id, order_no, pay_order_no, member_id, amount, channel, status, third_order_no, pay_time, expire_time, create_time) VALUES
(1, 'O202607180001', 'PAY202607180001', 1, 268.00, 'WECHAT_NATIVE', 2, 'WX202607100001', '2026-07-10 10:00:00', '2026-07-10 12:00:00', '2026-07-10 09:55:00'),
(2, 'O202607180002', 'PAY202607180002', 1, 318.00, 'WECHAT_NATIVE', 2, 'WX202607150001', '2026-07-15 14:00:00', '2026-07-15 15:50:00', '2026-07-15 13:50:00'),
(3, 'O202607180003', 'PAY202607180003', 2, 168.00, 'ALIPAY_WEB', 2, 'ALI202607120001', '2026-07-12 09:00:00', '2026-07-12 10:55:00', '2026-07-12 08:55:00'),
(4, 'O202607180004', 'PAY202607180004', 2, 499.00, 'WECHAT_NATIVE', 2, 'WX202607160001', '2026-07-16 11:00:00', '2026-07-16 12:50:00', '2026-07-16 10:50:00'),
(5, 'O202607180005', 'PAY202607180005', 3, 148.00, 'WECHAT_NATIVE', 2, 'WX202607110001', '2026-07-11 16:00:00', '2026-07-11 17:55:00', '2026-07-11 15:55:00'),
(6, 'O202607180006', 'PAY202607180006', 3, 108.00, 'WALLET', 2, NULL, '2026-07-14 08:00:00', '2026-07-14 09:50:00', '2026-07-14 07:50:00'),
(7, 'O202607180007', 'PAY202607180007', 4, 268.00, 'WECHAT_NATIVE', 2, 'WX202607130001', '2026-07-13 10:00:00', '2026-07-13 11:55:00', '2026-07-13 09:55:00'),
(8, 'O202607180008', 'PAY202607180008', 5, 118.00, 'WECHAT_NATIVE', 2, 'WX202607140001', '2026-07-14 12:00:00', '2026-07-14 13:50:00', '2026-07-14 11:50:00'),
(9, 'O202607180009', 'PAY202607180009', 6, 359.00, 'WECHAT_NATIVE', 2, 'WX202607150002', '2026-07-15 09:00:00', '2026-07-15 10:50:00', '2026-07-15 08:50:00'),
(10, 'O202607180010', 'PAY202607180010', 8, 88.00, 'WECHAT_NATIVE', 2, 'WX202607160002', '2026-07-16 15:00:00', '2026-07-16 16:50:00', '2026-07-16 14:50:00'),
(11, 'O202607180011', 'PAY202607180011', 5, 228.00, 'WECHAT_NATIVE', 0, NULL, NULL, '2026-07-18 12:00:00', '2026-07-18 10:00:00')
ON DUPLICATE KEY UPDATE pay_order_no = VALUES(pay_order_no), amount = VALUES(amount), status = VALUES(status), third_order_no = VALUES(third_order_no), pay_time = VALUES(pay_time);

-- ============================================
-- 14. 会员第三方账号绑定
-- ============================================
INSERT INTO ds_member_social (id, member_id, social_type, union_id, open_id, nickname, avatar, create_time) VALUES
(1, 1, 'wechat_web', 'union_zhangsan_001', 'open_zhangsan_web_001', '张三的微信', 'https://img.example.com/avatar/1.jpg', '2026-06-01 08:05:00'),
(2, 2, 'wechat_web', 'union_lisi_001', 'open_lisi_web_001', '李四的微信', 'https://img.example.com/avatar/2.jpg', '2026-06-10 10:05:00'),
(3, 2, 'wechat_miniapp', 'union_lisi_001', 'open_lisi_mp_001', '李四', 'https://img.example.com/avatar/2_mp.jpg', '2026-06-10 10:10:00'),
(4, 3, 'wechat_web', 'union_wangwu_001', 'open_wangwu_web_001', '王五的微信', 'https://img.example.com/avatar/3.jpg', '2026-06-12 14:05:00')
ON DUPLICATE KEY UPDATE social_type = VALUES(social_type), open_id = VALUES(open_id), nickname = VALUES(nickname);

-- ============================================
-- 15. 文件上传记录
-- ============================================
INSERT INTO ds_file_record (id, file_name, file_type, file_size, storage_type, file_path, access_url, module, biz_id, create_by, create_time) VALUES
(1, 'avatar_zhangsan.jpg', 'image/jpeg', 204800, 'local', '/data/uploads/avatar/2026/07/avatar_zhangsan.jpg', 'http://localhost:8080/uploads/avatar/2026/07/avatar_zhangsan.jpg', 'avatar', 1, 1, '2026-07-01 08:00:00'),
(2, 'product_1_main.jpg', 'image/jpeg', 512000, 'local', '/data/uploads/product/2026/07/product_1_main.jpg', 'http://localhost:8080/uploads/product/2026/07/product_1_main.jpg', 'product', 1, 1, '2026-07-01 10:00:00'),
(3, 'product_2_main.jpg', 'image/jpeg', 480000, 'local', '/data/uploads/product/2026/07/product_2_main.jpg', 'http://localhost:8080/uploads/product/2026/07/product_2_main.jpg', 'product', 2, 1, '2026-07-01 10:30:00'),
(4, 'product_3_main.jpg', 'image/jpeg', 450000, 'local', '/data/uploads/product/2026/07/product_3_main.jpg', 'http://localhost:8080/uploads/product/2026/07/product_3_main.jpg', 'product', 3, 1, '2026-07-01 11:00:00'),
(5, 'idcard_zhangsan_front.jpg', 'image/jpeg', 1024000, 'local', '/data/uploads/idcard/2026/07/idcard_zhangsan_front.jpg', 'http://localhost:8080/uploads/idcard/2026/07/idcard_zhangsan_front.jpg', 'idcard', 1, 1, '2026-07-02 09:00:00')
ON DUPLICATE KEY UPDATE file_name = VALUES(file_name), file_path = VALUES(file_path), access_url = VALUES(access_url);

-- ============================================
-- 16. 系统操作日志
-- ============================================
INSERT INTO ds_sys_log (id, user_id, username, module, action, description, ip, request_url, cost_time, create_time) VALUES
(1, 1, 'admin', '系统管理', 'LOGIN', '管理员登录系统', '127.0.0.1', '/api/admin/auth/login', 120, '2026-07-18 08:00:00'),
(2, 1, 'admin', '会员管理', 'VIEW', '查看会员列表', '127.0.0.1', '/api/admin/member/list', 85, '2026-07-18 08:05:00'),
(3, 1, 'admin', '商品管理', 'ADD', '新增商品：乳清蛋白粉 500g', '127.0.0.1', '/api/admin/product/add', 200, '2026-07-01 10:00:00'),
(4, 1, 'admin', '商品管理', 'ADD', '新增商品：植物蛋白粉 400g', '127.0.0.1', '/api/admin/product/add', 180, '2026-07-01 10:30:00'),
(5, 1, 'admin', '商品管理', 'ADD', '新增商品：复合维生素片 120粒', '127.0.0.1', '/api/admin/product/add', 150, '2026-07-01 11:00:00'),
(6, 1, 'admin', '奖金管理', 'GRANT', '发放2026-07周期奖金', '127.0.0.1', '/api/admin/bonus/grant', 3500, '2026-07-17 00:00:01'),
(7, 1, 'admin', '财务管理', 'AUDIT', '审核提现申请 TX202607180001', '127.0.0.1', '/api/admin/finance/withdraw/audit', 90, '2026-07-16 12:00:00'),
(8, 1, 'admin', '财务管理', 'GRANT', '打款提现 TX202607180001', '127.0.0.1', '/api/admin/finance/withdraw/grant', 300, '2026-07-16 14:00:00'),
(9, 1, 'admin', '系统管理', 'UPDATE', '修改系统配置', '127.0.0.1', '/api/admin/system/config/update', 60, '2026-07-15 10:00:00'),
(10, 1, 'admin', '会员管理', 'UPDATE', '修改会员等级', '127.0.0.1', '/api/admin/member/level/update', 75, '2026-07-10 10:00:00')
ON DUPLICATE KEY UPDATE description = VALUES(description), action = VALUES(action);

-- ============================================
-- 17. 更新商品销量
-- ============================================
UPDATE ds_product SET sales_count = (SELECT COUNT(*) FROM ds_order_item oi WHERE oi.product_id = ds_product.id);