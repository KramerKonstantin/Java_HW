1. Класс IterativeParallelism, который обрабатывает списки в несколько потоковю
2. Реализованы следующие методы:
minimum(threads, list, comparator) — первый минимум;
maximum(threads, list, comparator) — первый максимум;
all(threads, list, predicate) — проверка, что все элементы списка удовлетворяют предикату;
any(threads, list, predicate) — проверка, что существует элемент списка, удовлетворяющий предикату.
filter(threads, list, predicate) — вернуть список, содержащий элементы удовлетворяющие предикату;
map(threads, list, function) — вернуть список, содержащий результаты применения функции;
join(threads, list) — конкатенация строковых представлений элементов списка.
3. Во все функции передается параметр threads — сколько потоков надо использовать при вычислении.
4. Не используется Concurrency Utilities.
