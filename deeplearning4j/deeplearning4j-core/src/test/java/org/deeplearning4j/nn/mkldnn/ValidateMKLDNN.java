/*******************************************************************************
 * Copyright (c) 2015-2019 Skymind, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.deeplearning4j.nn.mkldnn;

import org.deeplearning4j.BaseDL4JTest;
import org.deeplearning4j.LayerHelperValidationUtil;
import org.deeplearning4j.TestUtils;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.junit.Test;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.Arrays;

import static org.junit.Assume.assumeTrue;

public class ValidateMKLDNN extends BaseDL4JTest {

    @Test
    public void validateConvSubsampling() throws Exception {
        //Only run test if using nd4j-native backend
        assumeTrue(Nd4j.getBackend().getClass().getName().toLowerCase().contains("native"));

        int[] inputSize = {-1, 3, 10, 10};

        for(int minibatch : new int[]{1,3}) {
            for (ConvolutionMode cm : ConvolutionMode.values()) {
                for (int[] kernel : new int[][]{{2, 2}, {2, 3}}) {
                    for (int[] stride : new int[][]{{1, 1}, {2, 2}}) {

                        inputSize[0] = minibatch;
                        INDArray f = Nd4j.rand(Nd4j.defaultFloatingPointType(), inputSize);
                        INDArray l = TestUtils.randomOneHot(minibatch, 10);

                        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                                .updater(new Adam(0.01))
                                .convolutionMode(cm)
                                .list()
                                .layer(new ConvolutionLayer.Builder().activation(Activation.TANH)
                                        .kernelSize(kernel)
                                        .stride(stride)
                                        .padding(0,0)
                                        .nOut(3)
                                        .build())
                                .layer(new SubsamplingLayer.Builder()
                                        .kernelSize(kernel)
                                        .stride(stride)
                                        .padding(0,0)
                                        .build())
                                .layer(new ConvolutionLayer.Builder().activation(Activation.TANH)
                                        .kernelSize(kernel)
                                        .stride(stride)
                                        .padding(0,0)
                                        .nOut(3)
                                        .build())
                                .layer(new OutputLayer.Builder().nOut(10).activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MCXENT).build())
                                .setInputType(InputType.convolutional(inputSize[2], inputSize[3], inputSize[1]))
                                .build();

                        MultiLayerNetwork netWith = new MultiLayerNetwork(conf.clone());
                        netWith.init();

                        MultiLayerNetwork netWithout = new MultiLayerNetwork(conf.clone());
                        netWithout.init();

                        LayerHelperValidationUtil.TestCase tc = LayerHelperValidationUtil.TestCase.builder()
                                .allowHelpersForClasses(Arrays.<Class<?>>asList(org.deeplearning4j.nn.layers.convolution.subsampling.SubsamplingLayer.class))
                                .testForward(true)
                                .testScore(true)
                                .testBackward(true)
                                .testTraining(true)
                                .features(f)
                                .labels(l)
                                .build();

                        LayerHelperValidationUtil.validateMLN(netWith, tc);
                    }
                }
            }
        }
    }
}