/*
**
** Author(s):
**  - Julien Freche <jfreche@aldebaran-robotics.com>
**
** Copyright (C) 2012 Aldebaran Robotics
*/

#ifndef	_QIMESSAGING_VALUE_C_P_H_
# define _QIMESSAGING_VALUE_C_P_H_

#include <qitype/genericvalue.hpp>

inline qi::GenericValue &qi_value_cpp(qi_value_t *value) {
  qi::GenericValue &gv = *(reinterpret_cast<qi::GenericValue *>(value));
  return gv;
};

#endif /* !_QIMESSAGING_VALUE_C_P_H_ */