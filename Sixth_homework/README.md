1. Класс ParallelMapperImpl, реализует интерфейс ParallelMapper.
public interface ParallelMapper extends AutoCloseable {
    <T, R> List<R> run(
        Function<? super T, ? extends R> f, 
        List<? extends T> args
    ) throws InterruptedException;

    @Override
    void close() throws InterruptedException;
}
2. Метод run параллельно вычисляет функцию f на каждом из указанных аргументов (args).
3. Метод close останавливает все рабочие потоки.
4. Конструктор ParallelMapperImpl(int threads) создает threads рабочих потоков, которые могут быть использованы для распараллеливания.
5. К одному ParallelMapperImpl могут одновременно обращаться несколько клиентов.
6. Задания на исполнение накапливается в очереди и обрабатывается в порядке поступления.
7. В реализации нет активных ожиданий.
8. Модифицировали касс IterativeParallelism так, чтобы он мог использовать ParallelMapper.
9. Добавили конструктор IterativeParallelism(ParallelMapper)
10. Методы класса делят работу на threads фрагментов и исполняют их при помощи ParallelMapper.
11. Есть возможность одновременного запуска и работы нескольких клиентов, использующих один ParallelMapper.
12. При наличии ParallelMapper сам IterativeParallelism новые потоки не создает.
