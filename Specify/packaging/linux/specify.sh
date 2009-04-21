#! /bin/sh


# add the libraries to the SPECIFY_CLASSPATH.
# EXEDIR is the directory where this executable is.
EXEDIR=${0%/*}
DIRLIBS=${EXEDIR}/Specify/libs/*.jar
for i in ${DIRLIBS}
do
  if [ -z "$SPECIFY_CLASSPATH" ] ; then
    SPECIFY_CLASSPATH=$i
  else
    SPECIFY_CLASSPATH="$i":$SPECIFY_CLASSPATH
  fi
done

# mmk: added ./Specify/config so that hadoop-site.xml will be found here instead of inside h.jar
#      this needs a different permanent home, though
SPECIFY_CLASSPATH=./Specify/config/:$SPECIFY_CLASSPATH

#DIRLIBS=${EXEDIR}/Specify/lib/*.zip
#for i in ${DIRLIBS}
#do
#  if [ -z "$SPECIFY_CLASSPATH" ] ; then
#    SPECIFY_CLASSPATH=$i
#  else
#    SPECIFY_CLASSPATH="$i":$SPECIFY_CLASSPATH
#  fi
#done

SPECIFY_CLASSPATH="${EXEDIR}/Specify/classes":$SPECIFY_CLASSPATH:"${EXEDIR}/Specify/help"
SPECIFY_HOME=$(pwd)

echo $SPECIFY_HOME
#echo $SPECIFY_CLASSPATH

JAVA_HOME=${EXEDIR}/jre

# mmk: added sun.java2d.pmoffscreen=false as a workaround for very slow performance over ssh
# see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4488401 
java -Dsun.java2d.pmoffscreen=false -Xmx512m -classpath "$SPECIFY_CLASSPATH:$CLASSPATH" -Dappdir=$SPECIFY_HOME/Specify -Dappdatadir=$SPECIFY_HOME -Djavadbdir=$SPECIFY_HOME/DerbyDatabases edu.ku.brc.specify.Specify "$@"
