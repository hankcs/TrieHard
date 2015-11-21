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

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * 通用Trie树的实现<br>
 * An implementation of a compact Trie. <br/>
 * <br/>
 * <i>From Wikipedia:</i> <br/>
 * <br/>
 * <code>
 * an ordered tree data structure that is used to store a dynamic set or associative array where the keys are usually strings. Unlike a binary search tree, no node in the tree stores the key associated with that node; instead, its position in the tree defines the key with which it is associated. All the descendants of a node have a common prefix of the string associated with that node, and the root is associated with the empty string. Values are normally not associated with every node, only with leaves and some inner nodes that correspond to keys of interest. For the space-optimized presentation of prefix tree, see compact prefix tree.
 * </code> <br/>
 * 
 * @author Philip Diffenderfer
 * 
 * @param <S>
 *        键类型<br>
 *        The sequence/key type.
 * @param <T>
 *        值类型<br>
 *        The value type.
 */
@SuppressWarnings ("unchecked" )
public class Trie<S, T> implements Map<S, T>
{

   /**
    * 返回空容器<br>
    * An empty collection/set to return.
    */
   private static final EmptyContainer<?> EMPTY_CONTAINER = new EmptyContainer<Object>();

   /**
    * 根节点
    */
   private final TrieNode<S, T> root;
   /**
    * hash计算器
    */
   private TrieSequencer<S> sequencer;
   /**
    * 默认匹配逻辑
    */
   private TrieMatch defaultMatch = TrieMatch.STARTS_WITH;
   /**
    * 序列集合
    */
   private SequenceSet sequences;
   /**
    * 值集合
    */
   private ValueCollection values;
   /**
    * 键值对集合
    */
   private EntrySet entries;
   /**
    * 节点集合
    */
   private NodeSet nodes;

   /**
    * Instantiates a new Trie.
    * 
    * @param sequencer
    *        The TrieSequencer which handles the necessary sequence operations.
    */
   public Trie( TrieSequencer<S> sequencer )
   {
      this( sequencer, null );
   }

   /**
    * 创建一个Trie树<br>
    * Instantiates a new Trie.
    * 
    * @param sequencer
    *        hash计算器<br>
    *        The TrieSequencer which handles the necessary sequence operations.
    * @param defaultValue
    *        匹配失败时的返回值<br>
    *        The default value of the Trie is the value returned when
    *        {@link #get(Object)} or {@link #get(Object, TrieMatch)} is called
    *        and no match was found.
    */
   public Trie( TrieSequencer<S> sequencer, T defaultValue )
   {
      this.root = new TrieNode<S, T>( null, defaultValue, null, 0, 0, new PerfectHashMap<TrieNode<S, T>>() );
      this.sequences = new SequenceSet( root );
      this.values = new ValueCollection( root );
      this.entries = new EntrySet( root );
      this.nodes = new NodeSet( root );
      this.sequencer = sequencer;
   }

   /**
    * 设置匹配失败时的返回值<br>
    * Sets the default value of the Trie, which is the value returned when a
    * query is unsuccessful.
    * 
    * @param defaultValue
    *        The default value of the Trie is the value returned when
    *        {@link #get(Object)} or {@link #get(Object, TrieMatch)} is called
    *        and no match was found.
    */
   public void setDefaultValue( T defaultValue )
   {
      root.value = defaultValue;
   }

   /**
    * 拷贝一个空的trie树<br>
    * Returns a Trie with the same default value, match, and
    * {@link TrieSequencer} as this Trie.
    * 
    * @return The reference to a new Trie.
    */
   public Trie<S, T> newEmptyClone()
   {
      Trie<S, T> t = new Trie<S, T>( sequencer, root.value );
      t.defaultMatch = defaultMatch;
      return t;
   }

   /**
    * put操作<br>
    * Puts the value in the Trie with the given sequence.
    * 
    * @param query
    *        序列<br>
    *        The sequence.
    * @param value
    *        值<br>
    *        The value to place in the Trie.
    * @return
    *         原来的值<br>
    *         The previous value in the Trie with the same sequence if one
    *         existed, otherwise null.
    */
   public T put( S query, T value )
   {
      final int queryLength = sequencer.lengthOf( query );

      if (value == null || queryLength == 0)
      {
         return null;
      }

      int queryOffset = 0;
      TrieNode<S, T> node = root.children.get( sequencer.hashOf( query, 0 ) );

      // 根节点没有该路径直达的子节点。The root doesn't have a child that starts with the given sequence...
      if (node == null)
      {
         // 创建该路径，直接连到根节点上。Add the sequence and value directly to root!
         return putReturnNull( root, value, query, queryOffset, queryLength );
      }

      while (node != null)
      {
         final S nodeSequence = node.sequence;
         final int nodeLength = node.end - node.start;
         final int max = Math.min( nodeLength, queryLength - queryOffset );
         final int matches = sequencer.matches( nodeSequence, node.start, query, queryOffset, max );

         queryOffset += matches;

         // 当前节点不是完全匹配的，且路径还没走完。mismatch in current node
         if (matches != max)
         {
            node.split( matches, null, sequencer );   // 枝条分割，创建一个分岔节点

            return putReturnNull( node, value, query, queryOffset, queryLength );   // 添加叶节点
         }

         // 当前节点不是完全匹配的，且路径走完了。partial match to the current node
         if (max < nodeLength)
         {
            node.split( max, value, sequencer );   // 枝条分割，创建一个叶节点
            node.sequence = query;

            return null;
         }

         // 当前节点是完全匹配的，且路径走完了，那么替换value和序列。Full match to query, replace value and sequence
         if (queryOffset == queryLength)
         {
            node.sequence = query;  // 这句看起来有点多余，可能是为了减小序列长度，优化内存而设的

            return node.setValue( value );
         }

         // 当前节点是完全匹配的，且路径还没走完，且没有子节点，那么新建叶子节点。full match, end of the query or node
         if (node.children == null)
         {
            return putReturnNull( node, value, query, queryOffset, queryLength );
         }

         // 当前节点是完全匹配的，且路径还没走完，且有子节点，那么看情况。full match, end of node
         TrieNode<S, T> next = node.children.get( sequencer.hashOf( query, queryOffset ) );

         if (next == null) // 没有对应的话就新建
         {
            return putReturnNull( node, value, query, queryOffset, queryLength );
         }

         // 否则继续往下走。full match, query or node remaining
         node = next;
      }

      return null;
   }

   /**
    * 创建一个从给定节点出发，通过给定路径到达的子节点<br>
    * Adds a new TrieNode to the given node with the given sequence subset.
    * 
    * @param node
    *        父节点<br>
    *        The node to add to; the parent of the created node.
    * @param value
    *        新子节点的值<br>
    *        The value of the node.
    * @param query
    *        路径<br>
    *        The sequence that was put.
    * @param queryOffset
    *        路径的开始<br>
    *        The offset into that sequence where the node (subset sequence)
    *        should begin.
    * @param queryLength
    *        完整路径的长度<br>
    *        The length of the subset sequence in elements.
    * @return null
    */
   private T putReturnNull( TrieNode<S, T> node, T value, S query, int queryOffset, int queryLength )
   {
      node.add( new TrieNode<S, T>( node, value, query, queryOffset, queryLength, null ), sequencer );

      return null;
   }

   /**
    * 获取指定序列对应的值<br>
    * Gets the value that matches the given sequence.
    * 
    * @param sequence
    *        指定序列<br>
    *        The sequence to match.
    * @param match
    *        匹配逻辑<br>
    *        The matching logic to use.
    * @return The value for the given sequence, or the default value of the Trie
    *         if no match was found. The default value of a Trie is by default
    *         null.
    */
   public T get( S sequence, TrieMatch match )
   {
      TrieNode<S, T> n = search( root, sequence, match );

      return (n != null ? n.value : root.value);
   }

   /**
    * 获取值，使用默认匹配逻辑<br>
    * Gets the value that matches the given sequence using the default
    * TrieMatch.
    * 
    * @param sequence
    *        The sequence to match.
    * @return The value for the given sequence, or the default value of the Trie
    *         if no match was found. The default value of a Trie is by default
    *         null.
    * @see #get(Object, TrieMatch)
    */
   public T get( Object sequence )
   {
      return get( (S)sequence, defaultMatch );
   }

   /**
    * 查看是否有这个序列作为key<br>
    * Determines whether a value exists for the given sequence.
    * 
    * @param sequence
    *        The sequence to match.
    * @param match
    *        The matching logic to use.
    * @return True if a value exists for the given sequence, otherwise false.
    */
   public boolean has( S sequence, TrieMatch match )
   {
      return hasAfter( root, sequence, match );
   }

   /**
    * 查看是否有这个序列作为key<br>
    * Determines whether a value exists for the given sequence using the default
    * TrieMatch.
    * 
    * @param sequence
    *        The sequence to match.
    * @return True if a value exists for the given sequence, otherwise false.
    * @see #has(Object, TrieMatch)
    */
   public boolean has( S sequence )
   {
      return hasAfter( root, sequence, defaultMatch );
   }

   /**
    * 从某个节点之后是否能匹配一个序列<br>
    * Starts at the root node and searches for a node with the given sequence
    * based on the given matching logic.
    * 
    * @param root
    *        The node to start searching from.
    * @param sequence
    *        The sequence to search for.
    * @param match
    *        The matching logic to use while searching.
    * @return True if root or a child of root has a match on the sequence,
    *         otherwise false.
    */
   protected boolean hasAfter( TrieNode<S, T> root, S sequence, TrieMatch match )
   {
      return search( root, sequence, match ) != null;
   }

   /**
    * 删除一个序列<br>
    * Removes the sequence from the Trie and returns it's value. The sequence
    * must be an exact match, otherwise nothing will be removed.
    * 
    * @param sequence
    *        The sequence to remove.
    * @return The value of the removed sequence, or null if no sequence was
    *         removed.
    */
   public T remove( Object sequence )
   {
      return removeAfter( root, (S)sequence );
   }

   /**
    * 从某个节点之后删除一个序列<br>
    * Starts at the root node and searches for a node with the exact given
    * sequence, once found it
    * removes it and returns the value. If a node is not found with the exact
    * sequence then null is returned.
    * 
    * @param root
    *        The root to start searching from.
    * @param sequence
    *        The exact sequence to search for.
    * @return The value of the removed node or null if it wasn't found.
    */
   protected T removeAfter( TrieNode<S, T> root, S sequence )
   {
      TrieNode<S, T> n = search( root, sequence, TrieMatch.EXACT );

      if (n == null)
      {
         return null;
      }

      T value = n.value;

      n.remove( sequencer );

      return value;
   }

   /**
    * 返回树大小<br>
    * Returns the number of sequences-value pairs in this Trie.
    * 
    * @return The number of sequences-value pairs in this Trie.
    */
   public int size()
   {
      return root.getSize();
   }

   /**
    * 是否为空<br>
    * Determines whether this Trie is empty.
    * 
    * @return 0 if the Trie doesn't have any sequences-value pairs, otherwise
    *         false.
    */
   public boolean isEmpty()
   {
      return (root.getSize() == 0);
   }

   /**
    * 获取默认匹配逻辑<br>
    * Returns the default TrieMatch used for {@link #has(Object)} and
    * {@link #get(Object)}.
    * 
    * @return The default TrieMatch set on this Trie.
    */
   public TrieMatch getDefaultMatch()
   {
      return defaultMatch;
   }

   /**
    * 设置默认匹配逻辑<br>
    * Sets the default TrieMatch used for {@link #has(Object)} and
    * {@link #get(Object)}.
    * 
    * @param match
    *        The new default TrieMatch to set on this Trie.
    */
   public void setDefaultMatch( TrieMatch match )
   {
      defaultMatch = match;
   }

   @Override
   public boolean containsKey( Object key )
   {
      return has( (S)key );
   }

   @Override
   public boolean containsValue( Object value )
   {
      Iterable<T> values = new ValueIterator( root );

      for (T v : values)
      {
         if (v == value || (v != null && value != null && v.equals( values )))
         {
            return true;
         }
      }

      return false;
   }

   @Override
   public Set<Entry<S, T>> entrySet()
   {
      return entries;
   }

   /**
    * 获取键值对<br>
    * Returns a {@link Set} of {@link Entry}s that match the given sequence
    * based on the default matching logic. If no matches were found then a
    * Set with size 0 will be returned. The set returned can have Entries
    * removed directly from it, given that the Entries are from this Trie.
    * 
    * @param sequence
    *        The sequence to match on.
    * @return The reference to a Set of Entries that matched.
    */
   public Set<Entry<S, T>> entrySet( S sequence )
   {
      return entrySet( sequence, defaultMatch );
   }

   /**
    * 获取键值对<br>
    * Returns a {@link Set} of {@link Entry}s that match the given sequence
    * based on the given matching logic. If no matches were found then a
    * Set with size 0 will be returned. The set returned can have Entries
    * removed directly from it, given that the Entries are from this Trie.
    * 
    * @param sequence
    *        The sequence to match on.
    * @param match
    *        The matching logic to use.
    * @return The reference to a Set of Entries that matched.
    */
   public Set<Entry<S, T>> entrySet( S sequence, TrieMatch match )
   {
      TrieNode<S, T> node = search( root, sequence, match );

      return (node == null ? (Set<Entry<S, T>>)EMPTY_CONTAINER : new EntrySet( node ));
   }

   /**
    * 所有节点<br>
    * The same as {@link #entrySet()} except instead of a {@link Set} of
    * {@link Entry}s, it's a {@link Set} of {@link TrieNode}s.
    * 
    * @return The reference to the Set of all valued nodes in this Trie.
    * @see #entrySet()
    */
   public Set<TrieNode<S, T>> nodeSet()
   {
      return nodes;
   }

   /**
    * 匹配某个序列得到的所有节点<br>
    * Returns a {@link Set} of {@link TrieNode}s that match the given sequence
    * based on the default matching logic. If no matches were found then a Set
    * with size 0 will be returned. The set returned can have TrieNodes removed
    * directly from it, given that the TrieNodes are from this Trie and they
    * will be removed from this Trie.
    * 
    * @param sequence
    *        The sequence to match on.
    * @return The reference to a Set of TrieNodes that matched.
    * @see #entrySet(Object)
    */
   public Set<TrieNode<S, T>> nodeSet( S sequence )
   {
      return nodeSet( sequence, defaultMatch );
   }

   /**
    * 匹配某个序列得到的所有节点<br>
    * Returns a {@link Set} of {@link TrieNode}s that match the given sequence
    * based on the given matching logic. If no matches were found then a Set
    * with size 0 will be returned. The set returned can have TrieNodes removed
    * directly from it, given that the TrieNodes are from this Trie.
    * 
    * @param sequence
    *        The sequence to match on.
    * @param match
    *        The matching logic to use.
    * @return The reference to a Set of TrieNodes that matched.
    * @see #entrySet(Object, TrieMatch)
    */
   public Set<TrieNode<S, T>> nodeSet( S sequence, TrieMatch match )
   {
      TrieNode<S, T> node = search( root, sequence, match );

      return (node == null ? (Set<TrieNode<S, T>>)EMPTY_CONTAINER : new NodeSet( node ));
   }

   /**
    * 获取所有节点，包含枝桠节点<br>
    * Returns an {@link Iterable} of all {@link TrieNode}s in this Trie
    * including naked (null-value) nodes.
    * 
    * @return The reference to a new Iterable.
    */
   public Iterable<TrieNode<S, T>> nodeSetAll()
   {
      return new NodeAllIterator( root );
   }

   /**
    * 获取匹配一段序列的所有节点，包含枝桠节点<br>
    * Returns an {@link Iterable} of all {@link TrieNode}s in this Trie that
    * match the given sequence using the default matching logic including naked
    * (null-value) nodes.
    * 
    * @param sequence
    *        The sequence to match on.
    * @return The reference to a new Iterable.
    */
   public Iterable<TrieNode<S, T>> nodeSetAll( S sequence )
   {
      return nodeSetAll( sequence, defaultMatch );

   }

   /**
    * 获取匹配一段序列的所有节点，包含枝桠节点<br>
    * Returns an {@link Iterable} of all {@link TrieNode}s in this Trie that
    * match the given sequence using the given matching logic including naked
    * (null-value) nodes.
    * 
    * @param sequence
    *        The sequence to match on.
    * @param match
    *        The matching logic to use.
    * @return The reference to a new Iterable.
    */
   public Iterable<TrieNode<S, T>> nodeSetAll( S sequence, TrieMatch match )
   {
      TrieNode<S, T> node = search( root, sequence, match );

      return (node == null ? (Iterable<TrieNode<S, T>>)EMPTY_CONTAINER : new NodeAllIterator( root ));
   }

   @Override
   public Set<S> keySet()
   {
      return sequences;
   }

   /**
    * 获取匹配一段序列的所有键<br>
    * Returns a {@link Set} of all keys (sequences) in this Trie that match the
    * given sequence given the default matching logic. If no matches were found
    * then a Set with size 0 will be returned. The Set returned can have
    * keys/sequences removed directly from it and they will be removed from this
    * Trie.
    * 
    * @param sequence
    *        The sequence to match on.
    * @return The reference to a Set of keys/sequences that matched.
    */
   public Set<S> keySet( S sequence )
   {
      return keySet( sequence, defaultMatch );
   }

   /**
    * 获取匹配一段序列的所有键<br>
    * Returns a {@link Set} of all keys (sequences) in this Trie that match the
    * given sequence with the given matching logic. If no matches were found
    * then a Set with size 0 will be returned. The Set returned can have
    * keys/sequences removed directly from it and they will be removed from this
    * Trie.
    * 
    * @param sequence
    *        The sequence to match on.
    * @param match
    *        The matching logic to use.
    * @return The reference to a Set of keys/sequences that matched.
    */
   public Set<S> keySet( S sequence, TrieMatch match )
   {
      TrieNode<S, T> node = search( root, sequence, match );

      return (node == null ? (Set<S>)EMPTY_CONTAINER : new SequenceSet( node ));
   }

   @Override
   public Collection<T> values()
   {
      return values;
   }

   /**
    * 获取匹配一段路径的所有值
    * @param sequence
    * @return
     */
   public Collection<T> values( S sequence )
   {
      return values( sequence, defaultMatch );
   }

   /**
    * 获取匹配一段路径的所有值
    * @param sequence
    * @param match
    * @return
     */
   public Collection<T> values( S sequence, TrieMatch match )
   {
      TrieNode<S, T> node = search( root, sequence, match );

      return (node == null ? null : new ValueCollection( node ));
   }

   @Override
   public void putAll( Map<? extends S, ? extends T> map )
   {
      for (Entry<? extends S, ? extends T> e : map.entrySet())
      {
         put( e.getKey(), e.getValue() );
      }
   }

   @Override
   public void clear()
   {
      root.children.clear();
      root.size = 0;
   }

   /**
    * 基于query和匹配逻辑执行搜索<br>
    * Searches in the Trie based on the sequence query and the matching logic.
    *
    * @param root
    *        查询开始的节点
    * @param query
    *        查询串<br>
    *        The query sequence.
    * @param match
    *        匹配逻辑<br>
    *        The matching logic.
    * @return The node that best matched the query based on the logic.
    */
   private TrieNode<S, T> search( TrieNode<S, T> root, S query, TrieMatch match )
   {
      final int queryLength = sequencer.lengthOf( query );

      // If the query is empty or matching logic is not given, return null.
      if (queryLength == 0 || match == null || queryLength < root.end)
      {
         return null;
      }

      int queryOffset = root.end;

      // If a non-root root was passed in, it might be the node you are looking for.
      if (root.sequence != null)
      {
         int matches = sequencer.matches( root.sequence, 0, query, 0, root.end );

         if (matches == queryLength)
         {
            return root;
         }
         if (matches < root.end)
         {
            return null;
         }
      }

      TrieNode<S, T> node = root.children.get( sequencer.hashOf( query, queryOffset ) );

      while (node != null)
      {
         final S nodeSequence = node.sequence;
         final int nodeLength = node.end - node.start;
         final int max = Math.min( nodeLength, queryLength - queryOffset );
         final int matches = sequencer.matches( nodeSequence, node.start, query, queryOffset, max );

         queryOffset += matches;

         // Not found
         if (matches != max)
         {
            return null;
         }

         // Potentially PARTIAL match
         if (max != nodeLength && matches == max)
         {
            return (match != TrieMatch.PARTIAL ? null : node);
         }

         // Either EXACT or STARTS_WITH match
         if (queryOffset == queryLength || node.children == null)
         {
            break;
         }

         TrieNode<S, T> next = node.children.get( sequencer.hashOf( query, queryOffset ) );

         // If there is no next, node could be a STARTS_WITH match
         if (next == null)
         {
            break;
         }

         node = next;
      }

      // EXACT matches
      if (node != null && match == TrieMatch.EXACT)
      {
         // Check length of last node against query
         if (node.value == null || node.end != queryLength)
         {
            return null;
         }

         // Check actual sequence values
         if (sequencer.matches( node.sequence, 0, query, 0, node.end ) != node.end)
         {
            return null;
         }
      }

      return node;
   }

   /**
    * 值集合
    */
   private class ValueCollection extends AbstractCollection<T>
   {

      private final TrieNode<S, T> root;

      public ValueCollection( TrieNode<S, T> root )
      {
         this.root = root;
      }

      @Override
      public Iterator<T> iterator()
      {
         return new ValueIterator( root );
      }

      @Override
      public int size()
      {
         return root.getSize();
      }
   }

   /**
    * 序列集合
    */
   private class SequenceSet extends AbstractSet<S>
   {

      private final TrieNode<S, T> root;

      public SequenceSet( TrieNode<S, T> root )
      {
         this.root = root;
      }

      @Override
      public Iterator<S> iterator()
      {
         return new SequenceIterator( root );
      }

      @Override
      public boolean remove( Object sequence )
      {
         return removeAfter( root, (S)sequence ) != null;
      }

      @Override
      public boolean contains( Object sequence )
      {
         return hasAfter( root, (S)sequence, TrieMatch.EXACT );
      }

      @Override
      public int size()
      {
         return root.getSize();
      }
   }

   /**
    * 键值对集合
    */
   private class EntrySet extends AbstractSet<Entry<S, T>>
   {

      private final TrieNode<S, T> root;

      public EntrySet( TrieNode<S, T> root )
      {
         this.root = root;
      }

      @Override
      public Iterator<Entry<S, T>> iterator()
      {
         return new EntryIterator( root );
      }

      @Override
      public boolean remove( Object entry )
      {
         TrieNode<S, T> node = (TrieNode<S, T>)entry;
         boolean removable = (node.getRoot() == Trie.this.root);

         if (removable)
         {
            node.remove( sequencer );
         }

         return removable;
      }

      @Override
      public boolean contains( Object entry )
      {
         TrieNode<S, T> node = (TrieNode<S, T>)entry;

         return (node.getRoot() == Trie.this.root);
      }

      @Override
      public int size()
      {
         return root.getSize();
      }
   }

   /**
    * 节点集合
    */
   private class NodeSet extends AbstractSet<TrieNode<S, T>>
   {

      private final TrieNode<S, T> root;

      public NodeSet( TrieNode<S, T> root )
      {
         this.root = root;
      }

      @Override
      public Iterator<TrieNode<S, T>> iterator()
      {
         return new NodeIterator( root );
      }

      @Override
      public boolean remove( Object entry )
      {
         TrieNode<S, T> node = (TrieNode<S, T>)entry;
         boolean removable = (node.getRoot() == Trie.this.root);

         if (removable)
         {
            node.remove( sequencer );
         }

         return removable;
      }

      @Override
      public boolean contains( Object entry )
      {
         TrieNode<S, T> node = (TrieNode<S, T>)entry;

         return (node.getRoot() == Trie.this.root);
      }

      @Override
      public int size()
      {
         return root.getSize();
      }
   }

   /**
    * 序列迭代器
    */
   private class SequenceIterator extends AbstractIterator<S>
   {

      public SequenceIterator( TrieNode<S, T> root )
      {
         super( root );
      }

      @Override
      public S next()
      {
         return nextNode().sequence;
      }
   }

   /**
    * 值迭代器
    */
   private class ValueIterator extends AbstractIterator<T>
   {

      public ValueIterator( TrieNode<S, T> root )
      {
         super( root );
      }

      @Override
      public T next()
      {
         return nextNode().value;
      }
   }

   /**
    * 键值对迭代器
    */
   private class EntryIterator extends AbstractIterator<Entry<S, T>>
   {

      public EntryIterator( TrieNode<S, T> root )
      {
         super( root );
      }

      @Override
      public Entry<S, T> next()
      {
         return nextNode();
      }
   }

   /**
    * 节点迭代器
    */
   private class NodeIterator extends AbstractIterator<TrieNode<S, T>>
   {

      public NodeIterator( TrieNode<S, T> root )
      {
         super( root );
      }

      @Override
      public TrieNode<S, T> next()
      {
         return nextNode();
      }
   }

   /**
    * 另一个节点迭代器
    */
   private class NodeAllIterator extends AbstractIterator<TrieNode<S, T>>
   {

      public NodeAllIterator( TrieNode<S, T> root )
      {
         super( root );
      }

      @Override
      public TrieNode<S, T> next()
      {
         return nextNode();
      }

      @Override
      protected boolean isAnyNode()
      {
         return true;
      }
   }

   /**
    * 迭代器基类
    * @param <K> 迭代器返回类型
     */
   private abstract class AbstractIterator<K> implements Iterable<K>, Iterator<K>
   {
      /**
       * 根节点
       */
      private final TrieNode<S, T> root;
      /**
       * 上一个
       */
      private TrieNode<S, T> previous;
      /**
       * 下一个
       */
      private TrieNode<S, T> current;
      /**
       * 深度
       */
      private int depth;
      /**
       * 在某个深度的第一个节点在父节点的hashmap中的id
       */
      private int[] indices = new int[32];   // 这个32是硬编码的，当深度大于32的时候会出问题

      /**
       * 构造迭代器
       * @param root 根节点
        */
      public AbstractIterator( TrieNode<S, T> root )
      {
         this.root = root;
         this.reset();
      }

      /**
       * 重置迭代器
       * @return
        */
      public AbstractIterator<K> reset()
      {
         depth = 0;
         indices[0] = -1;

         if (root.value == null)
         {
            previous = root;
            current = findNext();
         }
         else
         {
            previous = null;
            current = root;
         }

         return this;
      }

      /**
       * 是否需要任何节点
       * @return
        */
      protected boolean isAnyNode()
      {
         return false;
      }

      /**
       * 是否有下一个
       * @return
        */
      public boolean hasNext()
      {
         return (current != null);
      }

      /**
       * 下一个节点
       * @return
        */
      public TrieNode<S, T> nextNode()
      {
         previous = current;
         current = findNext();
         return previous;
      }

      /**
       * 删除
       */
      public void remove()
      {
         previous.remove( sequencer );
      }

      /**
       * 查找当前节点对应的下一个节点
       * @return
        */
      private TrieNode<S, T> findNext()
      {
         if (indices[0] == root.children.capacity())
         {
            return null;
         }

         TrieNode<S, T> node = previous;
         boolean foundValue = false;

         if (node.children == null)
         {
            node = node.parent;
         }

         while (!foundValue)
         {
            final PerfectHashMap<TrieNode<S, T>> children = node.children;
            final int childCapacity = children.capacity();
            int id = indices[depth] + 1;

            while (id < childCapacity && children.valueAt( id ) == null)
            {
               id++;
            }

            if (id == childCapacity)
            {
               node = node.parent;
               depth--;

               if (depth == -1)
               {
                  node = null;
                  foundValue = true;
               }
            }
            else
            {
               indices[depth] = id;
               node = children.valueAt( id );

               if (node.hasChildren())
               {
                  indices[++depth] = -1;
               }

               if (node.value != null || isAnyNode())
               {
                  foundValue = true;
               }
            }
         }

         return node;
      }

      @Override
      public Iterator<K> iterator()
      {
         return this;
      }
   }

   /**
    * 空容器
    * @param <T> 元素类型
     */
   private static class EmptyContainer<T> extends AbstractCollection<T> implements Set<T>, Iterator<T>
   {

      @Override
      public Iterator<T> iterator()
      {
         return this;
      }

      @Override
      public int size()
      {
         return 0;
      }

      @Override
      public boolean hasNext()
      {
         return false;
      }

      @Override
      public T next()
      {
         return null;
      }

      @Override
      public void remove()
      {

      }
   }

}
