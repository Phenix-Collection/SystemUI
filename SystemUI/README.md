#mare add begin

1.ʹ��eclipse ������������SystemUI��SettingsLib��Keyguard
   a.ʹ��import�е� existing project into workspace
   b.SettingsLib��Keyguard���뵼�룬��SystemUI�й����������Դ�������src�����Ѿ����뵽systemUI��srcĿ¼��
   c.�����ϵͳ���ļ�ȫ����ext_libs�У����apk push��ϵͳ��������ش��󣬿��������ڵ�ǰϵͳ��¼������汾
   �͵�ǰ���̵İ���һ�µ��£�ִ��copy�Լ�outĿ¼��jar���滻���ɡ�

2.���루ʹ�� ant ���룩
   a.build_windows--֪ͨ��.xml =�� �Ҽ�  =�� run as ��ant build��
   b.���ܲ���ϵͳ <property name="sdk-folder" value="${env.Android_SDK_HOME}" />�����뵱ǰeclipse��һ�£�����뱨��
   �����޸ļ��ɣ�as�� <property name="sdk-folder" value="D:/Tools/Android/adt-bundle-windows-x86_64-20140702/sdk" />  ��
   c.����Ҳ����ֱ���Ҽ� run as ��Android Application��,����eclipse��������Ϊjdk�����������
   erro��com/android/dx/command/dexer/Main : Unsupported major.minor version 52.0 ������ֱ�Ӻ��ԣ�ʹ��ANT���뼴�ɡ�

3.���ӣ�eclipseһ��Ĭ�ϼ���ant���������û�еĻ��������ant�������ɡ�

#mare add end