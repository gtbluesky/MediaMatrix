// 仿抖音幻觉特效
precision highp float;
varying vec2 vTextureCoord;
uniform sampler2D uTextureUnit;
uniform sampler2D uLutTextureUnit;
uniform sampler2D uLastFrameTextureUnit;

// 分RGB通道混合，不同颜色通道混合值不一样
const lowp vec3 blendValue = vec3(0.1, 0.3, 0.6);

vec4 getLutColor(vec4 textureColor) {
    // 一张LUT图大小为：512 * 512，共有：8 * 8 = 64个单元格，则单元格对应的索引范围：[0, 63]
    // 对应二维数组[8][8]
    // 将每个单元格垂直叠加起来得到一个立方体（实际是RGB对应XYZ轴的颜色映射组成的立方体，从Z轴均匀取64份）
    // 每个单元格中的横坐标（从左到右）即是R值，纵坐标（从上到下）即是G值
    highp float blueColor = textureColor.b * 63.0;
    // 计算相邻的两个B通道所在的单元格
    highp vec2 quad1;
    // 通过一维数组的索引确定二维数组的行号
    quad1.y = floor(floor(blueColor) / 8.0);
    // 通过一维数组的索引确定二维数组的列号
    quad1.x = floor(blueColor) - (quad1.y * 8.0);

    highp vec2 quad2;
    quad2.y = floor(ceil(blueColor) / 8.0);
    quad2.x = ceil(blueColor) - (quad2.y * 8.0);

    // 确定需要映射的单元格在查找表中的位置（像素值）
    // 再除以512得到纹理坐标
    // textureColor.r * 63.0得到的像素不一定是整数值，通过+0.5实现四舍五入
    highp vec2 texPos1;
    texPos1.x = (quad1.x / 8.0) + 0.5 / 512.0 + (textureColor.r * 63.0 / 512.0);
    texPos1.y = (quad1.y / 8.0) + 0.5 / 512.0 + (textureColor.g * 63.0 / 512.0);

    highp vec2 texPos2;
    texPos2.x = (quad2.x / 8.0) + 0.5 / 512.0 + (textureColor.r * 63.0 / 512.0);
    texPos2.y = (quad2.y / 8.0) + 0.5 / 512.0 + (textureColor.g * 63.0 / 512.0);

    lowp vec4 newColor1 = texture2D(uLutTextureUnit, texPos1);
    lowp vec4 newColor2 = texture2D(uLutTextureUnit, texPos2);

    lowp vec4 newColor = mix(newColor1, newColor2, fract(blueColor));
    return vec4(newColor.rgb, textureColor.a);
}

void main() {
    vec4 currentColor = texture2D(uTextureUnit, vTextureCoord);
    vec4 lastColor = texture2D(uLastFrameTextureUnit, vTextureCoord);
    vec4 lutColor = getLutColor(currentColor);
    gl_FragColor = vec4(mix(lastColor.rgb, lutColor.rgb, blendValue), currentColor.a);
}