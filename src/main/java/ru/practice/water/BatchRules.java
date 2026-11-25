package ru.practice.water;

final class BatchRules {
    static int good(int filled, int rejected) {
        if (filled <= 0 || rejected < 0 || rejected > filled) {
            throw new IllegalArgumentException("Проверьте выпуск и брак: выпуск должен быть положительным, брак — от нуля до количества выпущенных бутылок.");
        }
        return filled - rejected;
    }
    static int available(int filled, int rejected, Boolean approved, boolean received) {
        int good = good(filled, rejected);
        return Boolean.TRUE.equals(approved) && !received ? good : 0;
    }
}
