-- Dat lai mat khau admin va user mac dinh (dev).
UPDATE users SET password = '$2y$10$WXeb6ZprUVSBYBx2yXfQPOKNLnbaMIlb7VAXfbUUE924ltqoJBRYK' WHERE username = 'admin';
UPDATE users SET password = '$2y$10$MYYxIZ8FyEbTXytYPHT.eOOzfZQSDYGMrxUL6NyU3YHq1nSZxIQSC' WHERE username = 'user';
