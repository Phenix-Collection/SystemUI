#weiliji add begin

1.使用eclipse 导入三个工程SystemUI，SettingsLib，Keyguard
   a.使用import中的 existing project into workspace
   b.SettingsLib，Keyguard必须导入，在SystemUI中关联引入的资源，其相关src代码已经放入到systemUI的src目录中
   c.引入的系统库文件全部在ext_libs中，如果apk push到系统中遇到相关错误，可能是由于当前系统烧录的软件版本
   和当前工程的包不一致导致，执行copy自己out目录的jar包替换即可。

2.编译（使用 ant 编译）
   a.build_windows--通知栏.xml =》 右键  =》 run as “ant build”
   b.可能部分系统 <property name="sdk-folder" value="${env.Android_SDK_HOME}" />参数与当前eclipse不一致，会编译报错，
   自行修改即可（as： <property name="sdk-folder" value="D:/Tools/Android/adt-bundle-windows-x86_64-20140702/sdk" />  ）
   c.工程也可以直接右键 run as “Android Application”,部分eclipse环境会因为jdk环境问题出现
   erro：com/android/dx/command/dexer/Main : Unsupported major.minor version 52.0 【可以直接忽略，使用ANT编译即可】

3.附加：eclipse一般默认集成ant环境，如果没有的话自行添加ant环境即可。

#weiliji add end