/* 
 * NOTICE OF LICENSE
 * 
 * This source file is subject to the Open Software License (OSL 3.0) that is 
 * bundled with this package in the file LICENSE.txt. It is also available 
 * through the world-wide-web at http://opensource.org/licenses/osl-3.0.php
 * If you did not receive a copy of the license and are unable to obtain it 
 * through the world-wide-web, please send an email to magnos.software@gmail.com 
 * so we can send you a copy immediately. If you use any of this software please
 * notify me via our website or email, your feedback is much appreciated. 
 * 
 * @copyright   Copyright (c) 2011 Magnos Software (http://www.magnos.org)
 * @license     http://opensource.org/licenses/osl-3.0.php
 *              Open Software License (OSL 3.0)
 */

package org.magnos.trie;



/**
 * 对一个S序列进行操作的工具，一个序列指的是抽象的元素的线性排列<br>
 * A TrieSequencer enables a Trie to use keys of type S. A sequence is a
 * linear set of elements.
 * 
 * @author Philip Diffenderfer
 * 
 * @param <S>
 *        序列类型<br>
 *        The sequence type.
 */
public interface TrieSequencer<S>
{

   /**
    * 计算序列A和B从指定位置开始，最多一定个数，有多少个相同的元素<br>
    * Determines the maximum number of elements that match between sequences A
    * and B where comparison starts at the given indices up to the given count.
    * 
    * @param sequenceA
    *        第一个序列<br>
    *        The first sequence to count matches on.
    * @param indexA
    *        第一个序列开始位置<br>
    *        The offset into the first sequence.
    * @param sequenceB
    *        第二个序列<br>
    *        The second sequence to count matches on.
    * @param indexB
    *        第二个序列开始的位置<br>
    *        The offset into the second sequence.
    * @param count
    *        最多计算数量<br>
    *        The maximum number of matches to search for.
    * @return 一个介于(0, count)之间的数，代表两个序列之间的匹配元素数量
    *        A number between 0 (inclusive) and count (inclusive) that is the
    *         number of matches between the two sequence sections.
    */
   int matches(S sequenceA, int indexA, S sequenceB, int indexB, int count);

   /**
    * 计算给定序列的长度<br>
    * Calculates the length (number of elements) of the given sequence.
    * 
    * @param sequence
    *        待计算的序列<br>
    *        The sequence to measure.
    * @return 序列的长度
    *        The length of the given sequence.
    */
   int lengthOf(S sequence);

   /**
    * 计算给定序列给定位置的元素的hash值<br>
    * Calculates the hash of the element at the given index in the given
    * sequence. The hash is used as a key for the {@link PerfectHashMap} used
    * internally in a Trie to quickly retrieve entries. Typical implementations
    * based on characters return the ASCII value of the character, since it
    * yields dense numerical values. The more dense the hashes returned (the
    * smaller the difference between the minimum and maximum returnable hash
    * means it's more dense), the less space that is wasted.
    * 
    * @param sequence
    *        序列<br>
    *        The sequence.
    * @param index
    *        下标<br>
    *        The index of the element to calculate the hash of.
    * @return 该元素的hash<br>
    *        The hash of the element in the sequence at the index.
    */
   int hashOf(S sequence, int index);
   
}
