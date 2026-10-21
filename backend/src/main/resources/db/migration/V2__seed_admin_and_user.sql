-- Tai khoan mac dinh cho moi truong dev. Doi mat khau (hoac xoa migration nay) truoc khi len production.
INSERT INTO users (username, email, full_name, password, role, enabled, created_at) VALUES
    ('admin', 'admin@example.com', 'System Admin', '$2y$10$ioA45ezMGgiBitBuQ7g2iOc2oG3tSr7rv6pZ7/30sTQANxvfmzMze', 'ADMIN', true, now()),
    ('user', 'user@example.com', 'Demo User', '$2y$10$Fu87Jcn.BbYUCUV7BqMjOevOmOG38u0QwZmmg83PddmTPSePkWDsy', 'EMPLOYEE', true, now())
ON CONFLICT DO NOTHING;
