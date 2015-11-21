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

import java.util.Map.Entry;


/**
 * 一个TrieNode就是一个{@link java.util.Map.Entry Entry}，储存了key、value、key的区间、孩子数量、父节点引用<br>
 * A TrieNode is an {@link java.util.Map.Entry Entry} in a Trie that stores the
 * sequence (key), value, the starting and ending indices into the sequence, the
 * number of children in this node, and the parent to this node.
 * <p>
 * There are three types of TrieNodes and each have special properties.
 * </p>
 * <ol>
 * <li>Root
 * <ul>
 * <li>{@link #getStart()} == {@link #getEnd()} == 0</li>
 * <li>{@link #getValue()} == null</li>
 * <li>{@link #getKey()} == {@link #getSequence()} == null</li>
 * </ul>
 * </li>
 * <li>Naked Branch
 * <ul>
 * <li>{@link #getStart()} &lt; {@link #getEnd()}</li>
 * <li>{@link #getValue()} == null</li>
 * <li>{@link #getKey()} == {@link #getSequence()} == (a key of one of it's
 * children or a past child, ignore)</li>
 * </ul>
 * </li>
 * <li>Valued (Branch or Leaf)
 * <ul>
 * <li>{@link #getStart()} &lt; {@link #getEnd()}</li>
 * <li>{@link #getValue()} == non-null value passed into
 * {@link Trie#put(Object, Object)}</li>
 * <li>{@link #getKey()} == {@link #getSequence()} == a non-null key passed into
 * {@link Trie#put(Object, Object)}</li>
 * </ul>
 * </li>
 * </ol>
 * <p>
 * You can tell a valued branch or leaf apart by {@link #getChildCount()}, if it
 * returns 0 then it's a leaf, otherwise it's a branch.
 * </p>
 * 
 * 
 * @author Philip Diffenderfer
 * 
 */
public class TrieNode<S, T> implements Entry<S, T>
{
   /**
    * 父节点
    */
   protected TrieNode<S, T> parent;
   /**
    * 值
    */
   protected T value;
   /**
    * 序列
    */
   protected S sequence;
   /**
    * 序列的起点
    */
   protected int start;
   /**
    * 序列的终点
    */
   protected int end;
   /**
    * 完美hash表
    */
   protected PerfectHashMap<TrieNode<S, T>> children = null;
   /**
    * 所有子节点（子树）非空值（包含本节点的值）
    */
   protected int size;

   /**
    * 创建一个新的节点
    * Instantiates a new TrieNode.
    * 
    * @param parent
    *        父节点<br>
    *        The parent to this node.
    * @param value
    *        值<br>
    *        The value of this node.
    * @param sequence
    *        序列<br>
    *        The sequence of this node.
    * @param start
    *        本节点对应序列的起点，也是父节点对应序列的终点<br>
    *        The start of the sequence for this node, typically the end of the
    *        parent.
    * @param end
    *        本节点对应序列的终点<br>
    *        The end of the sequence for this node.
    * @param children
    *        初始子节点<br>
    *        The intial set of children.
    */
   protected TrieNode( TrieNode<S, T> parent, T value, S sequence, int start, int end, PerfectHashMap<TrieNode<S, T>> children )
   {
      this.parent = parent;
      this.sequence = sequence;
      this.start = start;
      this.end = end;
      this.children = children;
      this.size = calculateSize( children );
      this.setValue( value );
   }

   /**
    * 分割枝条，从指定位置开始，返回本节点对应的唯一子节点，该子节点继承了序列、值和孩子节点，但起点从index开始<br>
    * Splits this node at the given relative index and returns the TrieNode with
    * the sequence starting at index. The returned TrieNode has this node's
    * sequence, value, and children. The returned TrieNode is also the only
    * child of this node when this method returns.
    * 
    * @param index
    *        绝对下标（从0开始到end - start - 1中的一个值）<br>
    *        The relative index (starting at 0 and going to end - start - 1) in
    *        the sequence.
    * @param newValue
    *        新节点的值<br>
    *        The new value of this node.
    * @param sequencer
    *        序列处理器，用来计算hash值并放入map里面<br>
    *        The sequencer used to add the returned node to this node.
    * @return 指向孩子节点的引用<br>
    *         The reference to the child node created that's sequence starts at
    *         index.
    * 
    */
   protected TrieNode<S, T> split( int index, T newValue, TrieSequencer<S> sequencer )
   {
      TrieNode<S, T> c = new TrieNode<S, T>( this, value, sequence, index + start, end, children );
      c.registerAsParent();

      setValue( null );
      setValue( newValue );
      end = index + start;
      children = null;

      add( c, sequencer );

      return c;
   }

   /**
    * 将指定节点添加为子节点，子节点必须已经设置父节点为本节点，这样做是为了保证size
    * 正确<br>
    * Adds the given child to this TrieNode. The child TrieNode is expected to
    * have had this node's reference passed to it's constructor as the parent
    * parameter. This needs to be done to keep the size calculations accurate.
    * 
    * @param child
    *        子节点<br>
    *        The TrieNode to add as a child.
    * @param sequencer
    *        hash计算工具<br>
    *        The sequencer to use to determine the place of the node in the
    *        children PerfectHashMap.
    */
   protected void add( TrieNode<S, T> child, TrieSequencer<S> sequencer )
   {
      int hash = sequencer.hashOf( child.sequence, end );

      if (children == null)
      {
         children = new PerfectHashMap<TrieNode<S, T>>( hash, child );
      }
      else
      {
         children.put( hash, child );
      }
   }

   /**
    * 将此节点从父节点中删除，并且恰当地调整父子节点<br>
    * Removes this node from the Trie and appropriately adjusts it's parent and
    * children.
    * 
    * @param sequencer
    *        哈希计算工具<br>
    *        The sequencer to use to determine the place of this node in this
    *        nodes sibling PerfectHashMap.
    */
   protected void remove( TrieSequencer<S> sequencer )
   {
      // 减小size。Decrement size if this node had a value
      setValue( null );

      int childCount = (children == null ? 0 : children.size());

      // 当该节点没有子节点的时候，将该节点从它的父节点删除。When there are no children, remove this node from it's parent.
      if (childCount == 0)
      {
         parent.children.remove( sequencer.hashOf( sequence, start ) );
      }
      // 当该节点只有一个子节点的时候，自己变成子节点。With one child, become the child!
      else if (childCount == 1)
      {
         TrieNode<S, T> child = children.valueAt( 0 );

         children = child.children;
         value = child.value;
         sequence = child.sequence;
         end = child.end;

         child.children = null;
         child.parent = null;
         child.sequence = null;
         child.value = null;

         registerAsParent();
      }
   }

   /**
    * 增加size，同时增加父节点的size<br>
    * Adds the given size to this TrieNode and it's parents.
    * 
    * @param amount
    *        增加量<br>
    *        The amount of size to add.
    */
   private void addSize( int amount )
   {
      TrieNode<S, T> curr = this;

      while (curr != null)
      {
         curr.size += amount;
         curr = curr.parent;
      }
   }

   /**
    * 统计给定map中的非null节点<br>
    * Sums the sizes of all non-null TrieNodes in the given map.
    * 
    * @param nodes
    *        The map to calculate the total size of.
    * @return The total size of the given map.
    */
   private int calculateSize( PerfectHashMap<TrieNode<S, T>> nodes )
   {
      int size = 0;

      if (nodes != null)
      {
         for (int i = nodes.capacity() - 1; i >= 0; i--)
         {
            TrieNode<S, T> n = nodes.valueAt( i );

            if (n != null)
            {
               size += n.size;
            }
         }
      }

      return size;
   }

   /**
    * 确保所有子节点的父节点都指向本节点<br>
    * Ensures all child TrieNodes to this node are pointing to the correct
    * parent (this).
    */
   private void registerAsParent()
   {
      if (children != null)
      {
         for (int i = 0; i < children.capacity(); i++)
         {
            TrieNode<S, T> c = children.valueAt( i );

            if (c != null)
            {
               c.parent = this;
            }
         }
      }
   }

   /**
    * 查看本节点是否有子节点<br>
    * Returns whether this TrieNode has children.
    * 
    * @return True if children exist, otherwise false.
    */
   public boolean hasChildren()
   {
      return children != null && children.size() > 0;
   }

   /**
    * 返回父节点<br>
    * Returns the parent of this TrieNode. If this TrieNode doesn't have a
    * parent it signals that this TrieNode is the root of a Trie and null will
    * be returned.
    * 
    * @return The reference to the parent of this node, or null if this is a
    *         root node.
    */
   public TrieNode<S, T> getParent()
   {
      return parent;
   }

   /**
    * 返回值<br>
    * The value of this TrieNode.
    * 
    * @return The value of this TrieNode or null if this TrieNode is a branching
    *         node only (has children but the sequence in this node was never
    *         directly added).
    */
   @Override
   public T getValue()
   {
      return value;
   }

   /**
    * 返回完整序列，不过真实的到此节点的序列是从{@link #getStart()}开始到
    * {@link #getEnd()}终止的<br>
    * The complete sequence of this TrieNode. The actual sequence
    * is a sub-sequence that starts at {@link #getStart()} (inclusive) and ends
    * at {@link #getEnd()} (exclusive).
    * 
    * @return The complete sequence of this TrieNode.
    */
   public S getSequence()
   {
      return sequence;
   }

   /**
    * 获取序列的起点<br>
    * The start of the sequence in this TrieNode.
    * 
    * @return The start of the sequence in this TrieNode, greater than or equal
    *         to 0 and less than {@link #getEnd()}. In the case of
    *         the root node: {@link #getStart()} == {@link #getEnd()}.
    */
   public int getStart()
   {
      return start;
   }

   /**
    * 获取序列的终点<br>
    * The end of the sequence in this TrieNode.
    * 
    * @return The end of the sequence in this TrieNode, greater than
    *         {@link #getStart()}. In the case of the root node:
    *         {@link #getStart()} == {@link #getEnd()}.
    */
   public int getEnd()
   {
      return end;
   }

   /**
    * 返回所有子节点非空值（包含本节点的值）<br>
    * Returns the number of non-null values that exist in ALL child nodes
    * (including this node's value).
    * 
    * @return The number of non-null values and valid sequences.
    */
   public int getSize()
   {
      return size;
   }

   /**
    * 子节点数量<br>
    * Returns the number of direct children.
    * 
    * @return The number of direct children in this node.
    */
   public int getChildCount()
   {
      return (children == null ? 0 : children.size());
   }

   /**
    * 返回根节点<br>
    * Calculates the root node by traversing through all parents until it found
    * it.
    * 
    * @return The root of the {@link Trie} this TrieNode.
    */
   public TrieNode<S, T> getRoot()
   {
      TrieNode<S, T> n = parent;

      while (n.parent != null)
      {
         n = n.parent;
      }

      return n;
   }

   /**
    * 本节点是否是根节点<br>
    * @return True if this node is a root, otherwise false.
    */
   public boolean isRoot()
   {
      return (parent == null);
   }

   /**
    * 本节点是否是根节点或枝节点<br>
    * @return True if this node is a root or a naked (branch only) node,
    *         otherwise false.
    */
   public boolean isNaked()
   {
      return (value == null);
   }

   /**
    * 本节点是否有value<br>
    * @return True if this node has a non-null value (is not a root or naked
    *         node).
    */
   public boolean hasValue()
   {
      return (value != null);
   }

   @Override
   public S getKey()
   {
      return sequence;
   }

   @Override
   public T setValue( T newValue )
   {
      T previousValue = value;

      value = newValue;

      if (previousValue == null && value != null)
      {
         addSize( 1 );
      }
      else if (previousValue != null && value == null)
      {
         addSize( -1 );
      }

      return previousValue;
   }

   @Override
   public int hashCode()
   {
      return (sequence == null ? 0 : sequence.hashCode())
         ^ (value == null ? 0 : value.hashCode());
   }

   @Override
   public String toString()
   {
      return sequence + "=" + value;
   }

   @Override
   public boolean equals( Object o )
   {
      if (o == null || !(o instanceof TrieNode))
      {
         return false;
      }

      TrieNode<?, ?> node = (TrieNode<?, ?>)o;

      return (sequence == node.sequence || sequence.equals( node.sequence )) &&
         (value == node.value || (value != null && node.value != null && value.equals( node.value )));
   }

}
