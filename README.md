引用方式：
===

在root的build.gradle的allprojects下添加maven { url "https://jitpack.io" }
例子如下：
```c
allprojects {
	repositories {
		...
		maven { url "https://jitpack.io" }
	}
}
```

然后在app/build.gradle下的dependencies中，添加compile 'com.github.tqwboy:CoderTangHttp:v2.1.1'，例子如下：
```c
dependencies {
  compile 'com.github.tqwboy:CoderTangHttp:v2.1.1'
}
```

使用者还必须引用okhttp和okio这两个库。
