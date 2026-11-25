"""Процедурный расчёт партии розлива для сопоставления с Java."""
import argparse


def good_bottles(filled, rejected):
    if filled <= 0 or not 0 <= rejected <= filled:
        raise ValueError("Выпуск должен быть положительным, брак — от нуля до выпуска.")
    return filled - rejected


def available_for_receipt(filled, rejected, decision, received=False):
    good = good_bottles(filled, rejected)
    return good if decision == "approved" and not received else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("filled", type=int, help="Количество разлитых бутылок")
    parser.add_argument("rejected", type=int, help="Количество отбракованных бутылок")
    parser.add_argument("--decision", choices=["pending", "approved", "rejected"], default="pending")
    parser.add_argument("--received", action="store_true", help="Партия уже принята на склад")
    args = parser.parse_args()
    try:
        good = good_bottles(args.filled, args.rejected)
        available = available_for_receipt(args.filled, args.rejected, args.decision, args.received)
    except ValueError as error:
        parser.error(str(error))
    print(f"Годных бутылок: {good}. Доступно к приёмке: {available}.")


if __name__ == "__main__":
    main()
