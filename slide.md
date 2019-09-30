class: center, middle

# Java String Maniacs

---

## 自己紹介

![](https://avatars.githubusercontent.com/backpaper0?size=120)

- 名前：うらがみ
- 所属：TIS株式会社
- 趣味：ゼルダの伝説 ブレス オブ ザ ワイルド
- 得意：Java、Spring Boot

---

## 本日お話したかったこと

- 内部表現
- サロゲートペア、Unicode絵文字
- イミュータビリティ
- リテラルとプール、intern、GC

---

## 本日お話したかったこと

- 内部表現
- ~~サロゲートペア、Unicode絵文字~~
- ~~イミュータビリティ~~
- ~~リテラルとプール、intern、GC~~

こんなに喋る時間はない

---

class: center, middle

# 内部表現

---

## 内部表現

「Javaの文字列は内部的にはUTF-16で表現されている」

「文字列は`char`の配列で保持されている」

--

<b>Java 8までならそれは正しいが、<br/>
Java 9以降はちょっと違う</b>

---

class: center, middle

# JEP 254<br/>Compact Strings

---

## JEP 254: Compact Stringsとは

> Stringクラスの内部表現をUTF-16 char配列からバイト配列とエンコーディングフラグフィールドに変更することを提案します。
> 新しいStringクラスは文字列の内容に基づいてISO-8859-1 / Latin-1（文字あたり1バイト）またはUTF-16（文字あたり2バイト）としてエンコードされた文字を格納します。
> エンコーディングフラグは使用されているエンコーディングを示します。

https://openjdk.java.net/jeps/254

---

## JEP 254: Compact Stringsとは

つまり

```java
char[] value;
```

から

```java
byte[] value;
byte coder; //0ならLatin-1、1ならUTF-16
```

に変わった。

- [Java 8のString](https://hg.openjdk.java.net/jdk8u/jdk8u/jdk/file/b32eb8c2d908/src/share/classes/java/lang/String.java)
- [Java 9のString](https://hg.openjdk.java.net/jdk9/dev/jdk/file/65464a307408/src/java.base/share/classes/java/lang/String.java)

---

## なぜJEP 254が必要なのか？

`A`をUTF-16で表現すると

```
0x00 0x41
```

なのに対してLatin-1で表現すると

```
0x41
```

となる。

<b>
つまり1バイト節約できる。<br/>
約1,000文字あれば約1キロバイト節約できる。
</b>

---

class: center, middle

# 実装を見る

---

## 実装の概要

`String`のコンストラクタで値をLatin-1でエンコードしようと試みる。

エンコードできたらLatin-1、できなければUTF-16とする。

---

## 実装：Stringのコンストラクタ

```java
    public String(int[] codePoints, int offset, int count) {
        //※前提条件チェックのコード省略
        if (COMPACT_STRINGS) {
            byte[] val = StringLatin1.toBytes(codePoints, offset, count);
            if (val != null) {
                this.coder = LATIN1;
                this.value = val;
                return;
            }
        }
        this.coder = UTF16;
        this.value = StringUTF16.toBytes(codePoints, offset, count);
    }
```

- [Stringのコンストラクタ](https://hg.openjdk.java.net/jdk/jdk11/file/1ddf9a99e4ad/src/java.base/share/classes/java/lang/String.java#l312)

---

## 実装：StringLatin1.toBytes

```java
    public static byte[] toBytes(int[] val, int off, int len) {
        byte[] ret = new byte[len];
        for (int i = 0; i < len; i++) {
            int cp = val[off++];
            if (!canEncode(cp)) {
                return null;
            }
            ret[i] = (byte)cp;
        }
        return ret;
    }
```

- [StringLatin1.toBytes](https://hg.openjdk.java.net/jdk/jdk11/file/1ddf9a99e4ad/src/java.base/share/classes/java/lang/StringLatin1.java#l698)

---

## 実装：StringLatin1.canEncode

```java
    public static boolean canEncode(int cp) {
        return cp >>> 8 == 0;
    }
```

- [StringLatin1.canEncode](https://hg.openjdk.java.net/jdk/jdk11/file/1ddf9a99e4ad/src/java.base/share/classes/java/lang/StringLatin1.java#l52)

---

## ちょっとした懸念

最初にLatin-1でエンコードしようと試みるので、何文字も試した挙句に非Latin-1文字が出現した場合はパフォーマンスに影響があるのでは？

つまり`"ABC...XYZあいう"`のような場合。

※
実際のところ（計測はしていませんが）そんな文字列はあまり扱わないだろうから心配するようなことではないと思っています

---

## 必ずUTF-16にするフラグ

そんなパフォーマンスが心配なあなたに[String.COMPACT_STRINGS](https://hg.openjdk.java.net/jdk/jdk11/file/1ddf9a99e4ad/src/java.base/share/classes/java/lang/String.java#l198)。

こいつを`false`にすることでエンコードを必ずUTF-16にすることが可能。

システムプロパティで設定できる。

```sh
-XX:-CompactStrings
```

---

## ちなみに

> メモリ用の文字ストレージを最適化すると実行時のパフォーマンスの面でトレードオフが生じる場合があります。
> これはGCアクティビティの減少によって相殺され、一般的なサーバーベンチマークのスループットを維持できると予想しています。

https://openjdk.java.net/jeps/254

なるほど

---

## おわり

- 内部表現
- ~~サロゲートペア、Unicode絵文字~~
- ~~イミュータビリティ~~
- ~~リテラルとプール、intern、GC~~

話せなかったテーマについてはコミュニティのイベントでのネタにします。
