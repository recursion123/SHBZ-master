# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\zhang77555\Downloads\adt-bundle-windows-x86_64-20140321\adt-bundle-windows-x86_64-20140321\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-optimizationpasses 5          # ָ�������ѹ������
-dontusemixedcaseclassnames   # �Ƿ�ʹ�ô�Сд���
-dontpreverify           # ����ʱ�Ƿ���ԤУ��
-verbose                # ����ʱ�Ƿ��¼��־

-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*  # ����ʱ�����õ��㷨