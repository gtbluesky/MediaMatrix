precision highp float;
uniform sampler2D uTextureUnit;
varying lowp vec2 vTextureCoord;

uniform int width;
uniform int height;

// 磨皮程度
uniform float uSmoothStrength;
// 美白程度
uniform float uWhitenStrength;

float hardLight(float color) {
    if(color <= 0.5) {
        color = color * color * 2.0;
    } else {
        color = 1.0 - ((1.0 - color) * (1.0 - color) * 2.0);
    }
    return color;
}

void main() {
    vec3 centralColor = texture2D(uTextureUnit, vTextureCoord).rgb;
    vec2 singleStepOffset = vec2(1.0 / float(width), 1.0 / float(height));
    vec2 blurCoordinates[24];
    blurCoordinates[0] = vTextureCoord + singleStepOffset * vec2(0.0, -10.0);
    blurCoordinates[1] = vTextureCoord + singleStepOffset * vec2(0.0, 10.0);
    blurCoordinates[2] = vTextureCoord + singleStepOffset * vec2(-10.0, 0.0);
    blurCoordinates[3] = vTextureCoord + singleStepOffset * vec2(10.0, 0.0);
    blurCoordinates[4] = vTextureCoord + singleStepOffset * vec2(5.0, -8.0);
    blurCoordinates[5] = vTextureCoord + singleStepOffset * vec2(5.0, 8.0);
    blurCoordinates[6] = vTextureCoord + singleStepOffset * vec2(-5.0, 8.0);
    blurCoordinates[7] = vTextureCoord + singleStepOffset * vec2(-5.0, -8.0);
    blurCoordinates[8] = vTextureCoord + singleStepOffset * vec2(8.0, -5.0);
    blurCoordinates[9] = vTextureCoord + singleStepOffset * vec2(8.0, 5.0);
    blurCoordinates[10] = vTextureCoord + singleStepOffset * vec2(-8.0, 5.0);
    blurCoordinates[11] = vTextureCoord + singleStepOffset * vec2(-8.0, -5.0);
    blurCoordinates[12] = vTextureCoord + singleStepOffset * vec2(0.0, -6.0);
    blurCoordinates[13] = vTextureCoord + singleStepOffset * vec2(0.0, 6.0);
    blurCoordinates[14] = vTextureCoord + singleStepOffset * vec2(6.0, 0.0);
    blurCoordinates[15] = vTextureCoord + singleStepOffset * vec2(-6.0, 0.0);
    blurCoordinates[16] = vTextureCoord + singleStepOffset * vec2(-4.0, -4.0);
    blurCoordinates[17] = vTextureCoord + singleStepOffset * vec2(-4.0, 4.0);
    blurCoordinates[18] = vTextureCoord + singleStepOffset * vec2(4.0, -4.0);
    blurCoordinates[19] = vTextureCoord + singleStepOffset * vec2(4.0, 4.0);
    blurCoordinates[20] = vTextureCoord + singleStepOffset * vec2(-2.0, -2.0);
    blurCoordinates[21] = vTextureCoord + singleStepOffset * vec2(-2.0, 2.0);
    blurCoordinates[22] = vTextureCoord + singleStepOffset * vec2(2.0, -2.0);
    blurCoordinates[23] = vTextureCoord + singleStepOffset * vec2(2.0, 2.0);

    float distanceNormalizationFactor = 5.0;
    float gaussianWeightTotal = 0.2;

    float sampleColor = centralColor.g * gaussianWeightTotal;
    // 高斯模糊
    for (int i = 0; i < 24; ++i) {
        float sampler = texture2D(uTextureUnit, blurCoordinates[i]).g;
        float distanceFromCentralColor = min(abs(centralColor.g - sampler) * distanceNormalizationFactor, 1.0);
        float gaussianWeight = 0.1 * (1.0 - distanceFromCentralColor);
        gaussianWeightTotal += gaussianWeight;
        sampleColor += sampler * gaussianWeight;
    }
    sampleColor = sampleColor/gaussianWeightTotal;

    // 高反差保留
    float highPass = centralColor.g - sampleColor + 0.5;
    // 多次强光处理可以使得噪声更加突出
    for (int i = 0; i < 5; ++i) {
        highPass = hardLight(highPass);
    }

    float luminance = dot(centralColor, vec3(0.299, 0.587, 0.114));
    float alpha = pow(luminance, 0.4);
    vec3 smoothColor = centralColor + (centralColor - vec3(highPass)) * alpha * 0.1;
    smoothColor = clamp(smoothColor, vec3(0.0), vec3(1.0));

    // 磨皮
    smoothColor = mix(centralColor, smoothColor, alpha * uSmoothStrength);

    // 美白
    smoothColor.r += (1.0 - smoothColor.r) * uWhitenStrength * 0.3;
    smoothColor.g += (1.0 - smoothColor.g) * uWhitenStrength * 0.3;
    smoothColor.b += (1.0 - smoothColor.b) * uWhitenStrength * 0.3;

    gl_FragColor = vec4(smoothColor, 1.0);
}