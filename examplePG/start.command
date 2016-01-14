#!/bin/bash          
cd `dirname $0`
java -XstartOnFirstThread -cp coral-dep.jar:lib/swt-osx.jar coral.CoralHead
