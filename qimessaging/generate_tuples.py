def generate_tuple(n):
    output = ''
    output += 'package com.aldebaran.qimessaging;'
    output += 'public class Tuple%d <' % n
    output += ', '.join(['T' + str(i) for i in xrange(n)])
    output += '> extends Tuple {'
    for i in xrange(n):
        output += 'public T%d var%d;' % (i, i)
    output += 'public Tuple%d() {' % n
    for i in xrange(n):
        output += 'var%d = null;' % i
    output += '}'
    output += 'public Tuple%d(' % n
    output += ', '.join(['T%d arg%d' % (i, i) for i in xrange(n)])
    output += ') {'
    for i in xrange(n):
        output += 'var%d = arg%d;' % (i, i)
    output += '}'
    output += '''
  public <T> T get(int i) throws IndexOutOfBoundsException, ClassCastException, IllegalArgumentException, IllegalAccessException
  {
    return super.get(i);
  }

  public <T> void set(int index, T value) throws IllegalArgumentException, IllegalAccessException
  {
    super.<T>set(index, value);
  }
}'''
    return output

for i in xrange(1, 32+1):
    with open('Tuple%d.java' % i, 'w') as f:
        f.write(generate_tuple(i))
