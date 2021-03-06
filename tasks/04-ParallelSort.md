## ParallelSort
Консольное приложение, выполняющее сортировку строк входного потока.

```
java ParallelSort [-iu] [-t THREAD_COUNT] [-o OUTPUT] [FILES...]
```

Ключи:
* -i &mdash; выполнять сравнение без учёта регистра
* -u &mdash; выводить только уникальные строки
* -t &mdash; задаёт количество потоков для выполнения сортировки. Если параметр
не указан, программа должна сама выбрать необходимое количество потоков.
* -o &mdash; задаёт файл, в который необходимо вести запись результата. Если не
указан, то результат выводится в stdout.

Если приложение запускается без указания файлов, то считывание производится из
stdin. Результат работы приложения выводится в stdout/OUTPUT. Приложение должно
эффективно утилизировать ресурсы компьютера. Считается, что памяти достаточно для
осуществления сортировки и генерации результата в памяти, однако должно
расходоваться не больше, чем в два раза больше, чем суммарный объём всех файлов
(или считанных из stdin строк).

Тестовые данные находятся [здесь](https://github.com/dkomanov/fizteh-java-task/tree/master/tasks/parallelSort):
* ```wp.txt```: исходные сортируемые данные (слова из Войны и мира на трёх языках)
* ```wp_.txt```: отсортированные данные без ключей
* ```wp_i.txt```: отсортированные данные с ключами ```-i```
* ```wp_iu.txt```: отсортированные данные с ключами ```-iu```
* ```wp_u.txt```: отсортированные данные с ключами ```-u```

Соответственно, требуется стабильная сортировка.
