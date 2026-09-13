ALTER TABLE `task` MODIFY COLUMN `reward` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '完成奖励';

-- 把历史金额数据改写成自然的奖励文案
UPDATE `task` SET `reward` = CONCAT('一杯奶茶（', `reward`, ' 元）') WHERE `reward` REGEXP '^[0-9]+\\.?[0-9]*$' AND CAST(`reward` AS DECIMAL(10,2)) < 30;
UPDATE `task` SET `reward` = CONCAT('一顿大餐（', `reward`, ' 元）') WHERE `reward` REGEXP '^[0-9]+\\.?[0-9]*$' AND CAST(`reward` AS DECIMAL(10,2)) >= 30 AND CAST(`reward` AS DECIMAL(10,2)) < 100;
UPDATE `task` SET `reward` = CONCAT('一件心仪好物（', `reward`, ' 元）') WHERE `reward` REGEXP '^[0-9]+\\.?[0-9]*$' AND CAST(`reward` AS DECIMAL(10,2)) >= 100;
UPDATE `task` SET `reward` = '给自己一个小奖励' WHERE `reward` = '0' OR `reward` = '0.00';

-- 去掉金额中的 ".00" 小数尾巴
UPDATE `task` SET `reward` = REPLACE(`reward`, '.00', '') WHERE `reward` LIKE '%.00%';
