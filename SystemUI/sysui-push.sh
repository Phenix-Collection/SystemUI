#!/bin/sh
adb remount
adb push F:/code/qucii/qucii/SystemUI/bin/SystemUI.apk system/priv-app/SystemUI/
#adb shell am force-stop com.android.systemui
PROCESS=`adb shell ps |grep $com.android.systemui|grep -v grep|grep -v PPID |awk '{ print $2}'`
# com.android.systemui
# com.android.settings

for i in $PROCESS
do
  echo " adb shell Kill the $1 process [ $i ]"
  adb shell kill -9 $i
done

#  echo 按任意键继续
#read -n 1
