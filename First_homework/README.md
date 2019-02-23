1. Класс RecursiveWalk, осуществляет подсчет хеш-сумм файлов в директориях
2. Входной файл содержит список файлов и директорий, которые требуется обойти. Обход директорий осуществляется рекурсивно.
3. Пример.

Входной файл:

                        java/info/kgeorgiy/java/advanced/walk/samples/binary
                        java/info/kgeorgiy/java/advanced/walk/samples
                    
Выходной файл:

                        8e8881c5 java/info/kgeorgiy/java/advanced/walk/samples/binary
                        050c5d2e java/info/kgeorgiy/java/advanced/walk/samples/1
                        2076af58 java/info/kgeorgiy/java/advanced/walk/samples/12
                        72d607bb java/info/kgeorgiy/java/advanced/walk/samples/123
                        81ee2b55 java/info/kgeorgiy/java/advanced/walk/samples/1234
                        8e8881c5 java/info/kgeorgiy/java/advanced/walk/samples/binary

